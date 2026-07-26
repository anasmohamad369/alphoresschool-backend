package com.lumo.backend.quiz.dto;

public record QuestionGenerateRequest(
    String topic,
    Integer count
) {}
