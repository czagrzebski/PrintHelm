package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiChatRequest;
import com.czagrzebski.printhelm.model.ApiChatResponse;
import com.czagrzebski.printhelm.model.ApiChatSessionDetail;
import com.czagrzebski.printhelm.model.ApiChatSessionSummary;
import com.czagrzebski.printhelm.web.ai.PrinterChatService;
import com.czagrzebski.printhelm.web.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private PrinterChatService printerChatService;

    @Autowired
    private ChatSessionService chatSessionService;

    @PostMapping("/message")
    public ResponseEntity<ApiChatResponse> sendMessage(@RequestBody ApiChatRequest request, Principal principal) {
        return ResponseEntity.ok(printerChatService.chat(
                request.getContent(),
                request.getSessionId(),
                principal.getName(),
                request.getModel()));
    }

    @PostMapping("/message/stream")
    public SseEmitter chatStream(@RequestBody ApiChatRequest request, Principal principal) {
        SseEmitter emitter = new SseEmitter(120_000L);
        var ctx = SecurityContextHolder.getContext();
        Thread.ofVirtual().start(new DelegatingSecurityContextRunnable(() ->
                printerChatService.chatStream(
                        request.getContent(),
                        request.getSessionId(),
                        principal.getName(),
                        request.getModel(),
                        emitter), ctx));
        return emitter;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ApiChatSessionSummary>> getSessions(Principal principal) {
        return ResponseEntity.ok(chatSessionService.listSessions(principal.getName()));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiChatSessionDetail> getSession(@PathVariable String sessionId, Principal principal) {
        return ResponseEntity.ok(chatSessionService.getSession(sessionId, principal.getName()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId, Principal principal) {
        chatSessionService.deleteSession(sessionId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
