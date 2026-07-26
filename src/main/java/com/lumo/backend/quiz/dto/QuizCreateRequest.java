package com.lumo.backend.quiz.dto;

public record QuizCreateRequest(
    String title,
    String topic,
    String description,
    Long classId
) {}
