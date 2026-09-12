package com.lumo.backend.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record ConversationResponse(
    String participantId,
    String participantName,
    String participantRole,
    String participantSubtext,
    String lastMessage,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant lastActive,
    String lastSenderRole
) {
    public ConversationResponse(String participantId, String participantName, String lastMessage, Instant lastActive) {
        this(participantId, participantName, "USER", "", lastMessage, lastActive, "USER");
    }
}
