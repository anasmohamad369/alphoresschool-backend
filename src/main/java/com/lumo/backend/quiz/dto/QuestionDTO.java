package com.lumo.backend.quiz.dto;

public record QuestionDTO(
    String questionText,
    String optionA,
    String optionB,
    String optionC,
    String optionD,
    String correctAnswer
) {}
