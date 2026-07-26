package com.lumo.backend.quiz.controller;

import com.lumo.backend.quiz.dto.*;
import com.lumo.backend.quiz.entity.Quiz;
import com.lumo.backend.quiz.entity.QuizAttempt;
import com.lumo.backend.quiz.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping
    public ResponseEntity<Quiz> createQuiz(
            @RequestBody QuizCreateRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.createQuiz(request, authHeader));
    }

    @GetMapping
    public ResponseEntity<List<Quiz>> getAllQuizzes(
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.getAllQuizzes(authHeader));
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<Quiz> getQuizById(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.getQuizById(quizId, authHeader));
    }

    @PutMapping("/{quizId}")
    public ResponseEntity<Quiz> updateQuiz(
            @PathVariable Long quizId,
            @RequestBody QuizCreateRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.updateQuiz(quizId, request, authHeader));
    }

    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader
    ) {
        quizService.deleteQuiz(quizId, authHeader);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate-questions")
    public ResponseEntity<List<QuestionDTO>> generateQuestions(
            @RequestBody QuestionGenerateRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.generateAIQuestions(request, authHeader));
    }

    @GetMapping("/generate-questions")
    public ResponseEntity<List<QuestionDTO>> generateQuestionsGet(
            @RequestParam("topic") String topic,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestHeader("Authorization") String authHeader
    ) {
        QuestionGenerateRequest request = new QuestionGenerateRequest(topic, count);
        return ResponseEntity.ok(quizService.generateAIQuestions(request, authHeader));
    }

    @PostMapping("/{quizId}/questions")
    public ResponseEntity<Quiz> addQuestions(
            @PathVariable Long quizId,
            @RequestBody List<QuestionDTO> questions,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.addQuestions(quizId, questions, authHeader));
    }

    @PutMapping("/{quizId}/start")
    public ResponseEntity<Quiz> startQuiz(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.startQuiz(quizId, authHeader));
    }

    @PutMapping("/{quizId}/complete")
    public ResponseEntity<Quiz> completeQuiz(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.completeQuiz(quizId, authHeader));
    }

    @PutMapping("/{quizId}/request-activation")
    public ResponseEntity<Quiz> requestActivation(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.requestActivation(quizId, authHeader));
    }

    @GetMapping("/active")
    public ResponseEntity<List<Quiz>> getActiveQuizzes(
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.getActiveQuizzes(authHeader));
    }

    @PostMapping("/{quizId}/register")
    public ResponseEntity<QuizAttempt> registerForQuiz(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.registerForQuiz(quizId, authHeader));
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizAttempt> submitQuiz(
            @PathVariable Long quizId,
            @RequestBody QuizSubmitRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.submitQuiz(quizId, request, authHeader));
    }

    @GetMapping("/{quizId}/results")
    public ResponseEntity<List<QuizAttempt>> getQuizResults(
            @PathVariable Long quizId,
            @RequestHeader("Authorization") String authHeader
    ) {
        return ResponseEntity.ok(quizService.getQuizResults(quizId, authHeader));
    }
}
