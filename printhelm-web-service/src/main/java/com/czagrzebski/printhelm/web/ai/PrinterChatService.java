package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.model.ApiChatModel;
import com.czagrzebski.printhelm.model.ApiChatResponse;
import com.czagrzebski.printhelm.model.ApiProposedAction;
import com.czagrzebski.printhelm.web.domain.chat.ChatSession;
import com.czagrzebski.printhelm.web.service.ChatSessionService;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PrinterChatService {

    private static final String MODEL_HAIKU = "claude-haiku-4-5-20251001";
    private static final String MODEL_SONNET = "claude-sonnet-4-6";

    private final ChatClient chatClient;
    private final PrinterStateTool printerStateTool;
    private final PrinterControlTool printerControlTool;
    private final PrintQueueTool printQueueTool;
    private final ChatSessionService chatSessionService;

    public PrinterChatService(ChatClient.Builder builder,
                              PrinterStateTool printerStateTool,
                              PrinterControlTool printerControlTool,
                              PrintQueueTool printQueueTool,
                              ChatSessionService chatSessionService) {
        this.printerStateTool = printerStateTool;
        this.printerControlTool = printerControlTool;
        this.printQueueTool = printQueueTool;
        this.chatSessionService = chatSessionService;
        this.chatClient = builder
                .defaultSystem("""
                        You are PrintHelm AI, an intelligent assistant for managing 3D printers.
                        You have two categories of tools:

                        READ tools (execute immediately): listPrinters, getPrinterState, getPrinterQueue
                        - Use these freely to answer questions about printer status, temperatures, errors, progress, and queued jobs.

                        CONTROL tools (propose only — never execute): proposePause, proposeResume, proposeStop, proposeSetNozzleTemp, proposeSetBedTemp, proposeSetSpeed, proposeHome, proposeStartPrint
                        - These queue an action for the user to explicitly approve. They will NEVER execute automatically.
                        - Always call a control tool when the user asks you to perform a printer action.
                        - After proposing, tell the user to review and confirm the action shown below your message.
                        - For proposeStartPrint: \
                          1. Call getPrinterQueue to find a job with status READY_TO_PRINT and get its jobOrderId. \
                          2. Call getPrinterState to check for AMS trays (materialSystem.materials). \
                          3. If AMS trays are present, build amsMapping by matching each filament slot to a tray by type. \
                             If getPrinterState returned null, no materials, or the job has no filament metadata, pass amsMapping=[]. \
                          4. Always call proposeStartPrint — never skip it just because AMS info is unavailable.

                        Be concise and practical. If the user does not specify a printer by name, call listPrinters first.
                        """)
                .build();
    }

    public ApiChatResponse chat(String userContent, String sessionId, String username, ApiChatModel model) {
        ChatSession session = chatSessionService.loadOrCreate(sessionId, username);

        List<Message> messages = session.getMessages().stream()
                .map(m -> "user".equals(m.getRole())
                        ? (Message) new UserMessage(m.getContent())
                        : new AssistantMessage(m.getContent()))
                .toList();

        messages = new ArrayList<>(messages);
        messages.add(new UserMessage(userContent));

        String modelId = (model == ApiChatModel.SONNET) ? MODEL_SONNET : MODEL_HAIKU;

        printerControlTool.initRequest();
        printQueueTool.initRequest();

        String assistantContent = chatClient.prompt()
                .messages(messages)
                .tools(printerStateTool, printerControlTool, printQueueTool)
                .options(AnthropicChatOptions.builder().model(modelId).build())
                .call()
                .content();

        if (assistantContent == null) {
            assistantContent = "I'm sorry, I couldn't generate a response. Please try again.";
        }

        ChatSession saved = chatSessionService.save(session, userContent, assistantContent);

        List<ApiProposedAction> allActions = new ArrayList<>();
        allActions.addAll(printerControlTool.getAndClearActions());
        allActions.addAll(printQueueTool.getAndClearActions());

        ApiChatResponse response = new ApiChatResponse();
        response.setMessage(assistantContent);
        response.setSessionId(saved.getId());
        response.setProposedActions(allActions);
        return response;
    }
}
