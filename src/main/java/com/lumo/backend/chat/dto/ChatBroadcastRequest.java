package com.lumo.backend.chat.dto;

import java.util.List;

public record ChatBroadcastRequest(
    String targetRole,
    List<String> recipientIds,
    String content
) {
}
