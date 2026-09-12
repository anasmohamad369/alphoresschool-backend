package com.lumo.backend.chat.repository;

import com.lumo.backend.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
           "(m.senderId = :user2 AND m.receiverId = :user1) " +
           "ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistory(@Param("user1") String user1, @Param("user2") String user2);

    @Query(value = "SELECT * FROM chat_messages WHERE id IN (" +
           "  SELECT MAX(id) FROM chat_messages WHERE sender_id = :userId OR receiver_id = :userId " +
           "  GROUP BY CASE WHEN sender_id = :userId THEN receiver_id ELSE sender_id END" +
           ") ORDER BY timestamp DESC", nativeQuery = true)
    List<ChatMessage> findLatestMessagesForUser(@Param("userId") String userId);

    @Query(value = "SELECT * FROM chat_messages WHERE id IN (" +
           "  SELECT MAX(id) FROM chat_messages " +
           "  GROUP BY CASE WHEN sender_id < receiver_id THEN CONCAT(sender_id, ':', receiver_id) ELSE CONCAT(receiver_id, ':', sender_id) END" +
           ") ORDER BY timestamp DESC", nativeQuery = true)
    List<ChatMessage> findAllLatestConversations();
}
