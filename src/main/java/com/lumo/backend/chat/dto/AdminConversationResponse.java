package com.lumo.backend.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record AdminConversationResponse(
    String threadId,
    String teacherId,
    String teacherName,
    String teacherSubject,
    String teacherPhone,
    String studentId,
    String studentName,
    String parentName,
    String className,
    String sectionName,
    String parentPhone,
    String lastMessage,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant lastActive,
    String lastSenderRole
) {
}
