package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.chat.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    List<ChatSession> findByUsernameOrderByUpdatedAtDesc(String username);

    Optional<ChatSession> findByIdAndUsername(String id, String username);

    void deleteByIdAndUsername(String id, String username);
}
