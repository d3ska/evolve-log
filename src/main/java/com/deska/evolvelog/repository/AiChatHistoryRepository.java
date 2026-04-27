package com.deska.evolvelog.repository;

import com.deska.evolvelog.domain.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiChatHistoryRepository extends JpaRepository<AiChatMessage, UUID> {

    List<AiChatMessage> findTop10ByUserIdAndConversationIdOrderByCreatedAtDesc(UUID userId, UUID conversationId);
}
