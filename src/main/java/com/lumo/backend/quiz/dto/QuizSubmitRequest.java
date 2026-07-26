package com.lumo.backend.quiz.dto;

import java.util.List;

public record QuizSubmitRequest(
    List<SubmitAnswer> answers
) {}
