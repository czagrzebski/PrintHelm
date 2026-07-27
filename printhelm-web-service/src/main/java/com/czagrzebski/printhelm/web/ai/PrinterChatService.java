package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.model.ApiChatModel;
import com.czagrzebski.printhelm.model.ApiChatResponse;
import com.czagrzebski.printhelm.web.domain.chat.ChatSession;
import com.czagrzebski.printhelm.web.service.ChatSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrinterChatService {

    private static final String MODEL_HAIKU = "claude-haiku-4-5-20251001";
    private static final String MODEL_SONNET = "claude-sonnet-4-6";

    private static final String SYSTEM_PROMPT = """
            You are PrintHelm AI, an intelligent assistant for managing 3D printers.
            You have two categories of tools:

            READ tools (execute immediately): listPrinters, getPrinterState, getPrinterQueue
            - Use these freely to answer questions about printer status, temperatures, errors, progress, and queued jobs.

            CONTROL tools (propose only — never execute): proposePause, proposeResume, proposeStop, proposeSetNozzleTemp, proposeSetBedTemp, proposeSetSpeed, proposeHome, proposeStartPrint
            - These queue an action for the user to explicitly approve. They will NEVER execute automatically.
            - Always call a control tool when the user asks you to perform a printer action.
            - After proposing, tell the user to review and confirm the action shown below your message.
            - Do NOT say the action has been completed or succeeded — it is only queued for approval.
            - For proposeStartPrint: \
              1. Call getPrinterQueue to find a job with status READY_TO_PRINT and get its jobOrderId. \
              2. Call getPrinterState to check for AMS trays (materialSystem.materials). \
              3. If AMS trays are present, build amsMapping by matching each filament slot to a tray by type. \
                 If getPrinterState returned null, no materials, or the job has no filament metadata, pass amsMapping=[]. \
              4. Always call proposeStartPrint — never skip it just because AMS info is unavailable.

            When a [System] notice tells you a command was dispatched, you MUST call getPrinterState(printerId) \
            before responding. Report what you actually observe in the state — never assume the action succeeded.

            Be concise and practical. If the user does not specify a printer by name, call listPrinters first.
            """;

    private final ChatClient chatClient;
    private final ChatToolFactory chatToolFactory;
    private final ChatSessionService chatSessionService;
    private final ObjectMapper objectMapper;

    public PrinterChatService(ChatClient.Builder builder,
                              ChatToolFactory chatToolFactory,
                              ChatSessionService chatSessionService,
                              ObjectMapper objectMapper) {
        this.chatToolFactory = chatToolFactory;
        this.chatSessionService = chatSessionService;
        this.objectMapper = objectMapper;
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    private List<Message> buildMessageList(ChatSession session, String userContent) {
        List<Message> messages = new ArrayList<>(session.getMessages().stream()
                .map(m -> "user".equals(m.getRole())
                        ? (Message) new UserMessage(m.getContent())
                        : new AssistantMessage(m.getContent()))
                .toList());
        messages.add(new UserMessage(userContent));
        return messages;
    }

    public ApiChatResponse chat(String userContent, String sessionId, String username, ApiChatModel model) {
        ChatSession session = chatSessionService.loadOrCreate(sessionId, username);
        List<Message> messages = buildMessageList(session, userContent);
        String modelId = (model == ApiChatModel.SONNET) ? MODEL_SONNET : MODEL_HAIKU;

        ChatRequestContext ctx = new ChatRequestContext();

        String assistantContent = chatClient.prompt()
                .messages(messages)
                .tools(chatToolFactory.createTools(ctx))
                .options(AnthropicChatOptions.builder().model(modelId).build())
                .call()
                .content();

        if (assistantContent == null) {
            assistantContent = "I'm sorry, I couldn't generate a response. Please try again.";
        }

        ChatSession saved = chatSessionService.save(session, userContent, assistantContent);

        ApiChatResponse response = new ApiChatResponse();
        response.setMessage(assistantContent);
        response.setSessionId(saved.getId());
        response.setProposedActions(ctx.getActions());
        return response;
    }

    public void chatStream(String userContent, String sessionId, String username, ApiChatModel model, SseEmitter emitter) {
        ChatSession session = chatSessionService.loadOrCreate(sessionId, username);
        List<Message> messages = buildMessageList(session, userContent);
        String modelId = (model == ApiChatModel.SONNET) ? MODEL_SONNET : MODEL_HAIKU;

        // Status updates fire from the tool-execution thread while the stream is live.
        ChatRequestContext ctx = new ChatRequestContext(label -> sendEvent(emitter, "status", label));

        StringBuilder accumulated = new StringBuilder();

        try {
            chatClient.prompt()
                    .messages(messages)
                    .tools(chatToolFactory.createTools(ctx))
                    .options(AnthropicChatOptions.builder().model(modelId).build())
                    .stream()
                    .chatResponse()
                    .doOnNext(response -> {
                        if (response.getResults() == null || response.getResults().isEmpty()) {
                            return;
                        }
                        var result = response.getResult();

                        String token = result.getOutput() != null ? result.getOutput().getText() : null;
                        if (token != null && !token.isEmpty()) {
                            accumulated.append(token);
                            sendEvent(emitter, "token", token);
                        }

                        // The assistant writes text in segments across tool-call rounds
                        // ("let me look that up" → tool call → "here's what I found").
                        // Each round ends with a finish reason; without a separator the
                        // segments concatenate into "...printer ID.Now I'll propose...".
                        String finish = result.getMetadata() != null ? result.getMetadata().getFinishReason() : null;
                        if (finish != null && !finish.isBlank()
                                && !accumulated.isEmpty()
                                && accumulated.charAt(accumulated.length() - 1) != '\n') {
                            accumulated.append("\n\n");
                            sendEvent(emitter, "token", "\n\n");
                        }
                    })
                    .blockLast();

            String full = accumulated.toString().strip();
            ChatSession saved = chatSessionService.save(session, userContent, full);

            ApiChatResponse meta = new ApiChatResponse();
            meta.setSessionId(saved.getId());
            meta.setProposedActions(ctx.getActions());

            emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(meta)));
            emitter.complete();
        } catch (Exception e) {
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Sends an SSE event with the payload JSON-encoded. Raw SSE data cannot
     * represent leading spaces or newlines faithfully (the frontend parser and
     * the SSE spec both strip a leading space after "data:"), which was mangling
     * streamed tokens like " get" into "get". JSON encoding round-trips exactly.
     */
    private void sendEvent(SseEmitter emitter, String name, String payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
