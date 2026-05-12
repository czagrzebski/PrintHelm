package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiChatMessageItem;
import com.czagrzebski.printhelm.model.ApiChatSessionDetail;
import com.czagrzebski.printhelm.model.ApiChatSessionSummary;
import com.czagrzebski.printhelm.web.domain.chat.ChatMessageRecord;
import com.czagrzebski.printhelm.web.domain.chat.ChatSession;
import com.czagrzebski.printhelm.web.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    public ChatSession loadOrCreate(String sessionId, String username) {
        if (sessionId != null && !sessionId.isBlank()) {
            return chatSessionRepository.findByIdAndUsername(sessionId, username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        }
        ChatSession session = new ChatSession();
        session.setUsername(username);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        return session;
    }

    public ChatSession save(ChatSession session, String userContent, String assistantContent) {
        if (session.getTitle() == null) {
            session.setTitle(deriveTitle(userContent));
        }
        session.getMessages().add(new ChatMessageRecord("user", userContent));
        session.getMessages().add(new ChatMessageRecord("assistant", assistantContent));
        session.setUpdatedAt(Instant.now());
        return chatSessionRepository.save(session);
    }

    public List<ApiChatSessionSummary> listSessions(String username) {
        return chatSessionRepository.findByUsernameOrderByUpdatedAtDesc(username).stream()
                .map(this::toSummary)
                .toList();
    }

    public ApiChatSessionDetail getSession(String sessionId, String username) {
        ChatSession session = chatSessionRepository.findByIdAndUsername(sessionId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        return toDetail(session);
    }

    public void deleteSession(String sessionId, String username) {
        if (chatSessionRepository.findByIdAndUsername(sessionId, username).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        chatSessionRepository.deleteByIdAndUsername(sessionId, username);
    }

    private ApiChatSessionSummary toSummary(ChatSession session) {
        ApiChatSessionSummary summary = new ApiChatSessionSummary();
        summary.setSessionId(session.getId());
        summary.setTitle(session.getTitle());
        if (session.getCreatedAt() != null) {
            summary.setCreatedAt(OffsetDateTime.ofInstant(session.getCreatedAt(), ZoneOffset.UTC));
        }
        if (session.getUpdatedAt() != null) {
            summary.setUpdatedAt(OffsetDateTime.ofInstant(session.getUpdatedAt(), ZoneOffset.UTC));
        }
        return summary;
    }

    private ApiChatSessionDetail toDetail(ChatSession session) {
        ApiChatSessionDetail detail = new ApiChatSessionDetail();
        detail.setSessionId(session.getId());
        detail.setTitle(session.getTitle());
        if (session.getCreatedAt() != null) {
            detail.setCreatedAt(OffsetDateTime.ofInstant(session.getCreatedAt(), ZoneOffset.UTC));
        }
        if (session.getUpdatedAt() != null) {
            detail.setUpdatedAt(OffsetDateTime.ofInstant(session.getUpdatedAt(), ZoneOffset.UTC));
        }
        List<ApiChatMessageItem> messages = session.getMessages().stream()
                .map(m -> {
                    ApiChatMessageItem item = new ApiChatMessageItem();
                    item.setRole(ApiChatMessageItem.RoleEnum.fromValue(m.getRole()));
                    item.setContent(m.getContent());
                    return item;
                })
                .toList();
        detail.setMessages(messages);
        return detail;
    }

    private String deriveTitle(String firstMessage) {
        String trimmed = firstMessage.trim();
        return trimmed.length() <= 60 ? trimmed : trimmed.substring(0, 57) + "...";
    }
}
