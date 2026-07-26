package com.lumo.backend.quiz.dto;

public record SubmitAnswer(
    Long questionId,
    String selectedAnswer
) {}
