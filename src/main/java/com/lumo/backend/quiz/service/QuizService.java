package com.lumo.backend.quiz.service;

import com.lumo.backend.quiz.dto.*;
import com.lumo.backend.quiz.entity.*;
import com.lumo.backend.quiz.repository.*;
import com.lumo.backend.teachers.entity.Teacher;
import com.lumo.backend.teachers.repository.TeacherRepository;
import com.lumo.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TeacherRepository teacherRepository;
    private final JwtService jwtService;
    private final GeminiService geminiService;

    public QuizService(
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            TeacherRepository teacherRepository,
            JwtService jwtService,
            GeminiService geminiService
    ) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.teacherRepository = teacherRepository;
        this.jwtService = jwtService;
        this.geminiService = geminiService;
    }

    private String getHeaderToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token.");
        }
        return authHeader.substring(7).trim();
    }

    private Teacher getAuthenticatedTeacher(String authHeader) {
        String token = getHeaderToken(authHeader);
        String email = jwtService.extractTeacherSubject(token);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid teacher token.");
        }
        return teacherRepository.findByEmailId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Teacher account not found."));
    }

    private String getAuthenticatedStudentId(String authHeader) {
        String token = getHeaderToken(authHeader);
        String studentId = jwtService.extractStudentSubject(token);
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid student token.");
        }
        return studentId;
    }

    public Quiz createQuiz(QuizCreateRequest request, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);

        Quiz quiz = new Quiz();
        quiz.setTitle(request.title());
        quiz.setTopic(request.topic());
        quiz.setDescription(request.description());
        quiz.setStatus("DRAFT");
        quiz.setTeacherId(teacher.getId());

        return quizRepository.save(quiz);
    }

    public Quiz getQuizById(Long quizId, String authHeader) {
        String token = getHeaderToken(authHeader);
        String studentId = jwtService.extractStudentSubject(token);
        String teacherEmail = jwtService.extractTeacherSubject(token);
        if (studentId == null && teacherEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token.");
        }

        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found with id: " + quizId));
    }

    public List<Quiz> getAllQuizzes(String authHeader) {
        String token = getHeaderToken(authHeader);
        String studentId = jwtService.extractStudentSubject(token);
        String teacherEmail = jwtService.extractTeacherSubject(token);
        if (studentId == null && teacherEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token.");
        }

        return quizRepository.findAll();
    }

    public List<QuestionDTO> generateAIQuestions(QuestionGenerateRequest request, String authHeader) {
        // Verify teacher auth
        getAuthenticatedTeacher(authHeader);
        int count = request.count() != null ? request.count() : 5;
        return geminiService.generateQuestions(request.topic(), count);
    }

    public Quiz addQuestions(Long quizId, List<QuestionDTO> questionsDto, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!quiz.getTeacherId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator teacher can modify this quiz.");
        }

        // Replace questions
        quiz.getQuestions().clear();
        for (QuestionDTO dto : questionsDto) {
            Question question = new Question();
            question.setQuestionText(dto.questionText());
            question.setOptionA(dto.optionA());
            question.setOptionB(dto.optionB());
            question.setOptionC(dto.optionC());
            question.setOptionD(dto.optionD());
            question.setCorrectAnswer(dto.correctAnswer().toUpperCase().trim());
            quiz.getQuestions().add(question);
        }

        return quizRepository.save(quiz);
    }

    public Quiz startQuiz(Long quizId, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!quiz.getTeacherId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator teacher can start this quiz.");
        }

        quiz.setStatus("STARTED");
        return quizRepository.save(quiz);
    }

    public Quiz completeQuiz(Long quizId, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!quiz.getTeacherId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator teacher can complete this quiz.");
        }

        quiz.setStatus("COMPLETED");
        return quizRepository.save(quiz);
    }

    public List<Quiz> getActiveQuizzes() {
        return quizRepository.findByStatus("STARTED");
    }

    public QuizAttempt registerForQuiz(Long quizId, String authHeader) {
        String studentId = getAuthenticatedStudentId(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!"STARTED".equals(quiz.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz is not currently active.");
        }

        Optional<QuizAttempt> existing = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quizId);
        attempt.setStudentId(studentId);
        attempt.setScore(0);
        attempt.setCompleted(false);

        return quizAttemptRepository.save(attempt);
    }

    public QuizAttempt submitQuiz(Long quizId, QuizSubmitRequest request, String authHeader) {
        String studentId = getAuthenticatedStudentId(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!"STARTED".equals(quiz.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz has ended or is not active.");
        }

        QuizAttempt attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not registered for this quiz. Please register first."));

        if (Boolean.TRUE.equals(attempt.getCompleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz attempt has already been submitted.");
        }

        int score = 0;
        attempt.getAnswers().clear();

        for (SubmitAnswer answerReq : request.answers()) {
            Question question = quiz.getQuestions().stream()
                    .filter(q -> q.getId().equals(answerReq.questionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid question ID: " + answerReq.questionId()));

            boolean correct = question.getCorrectAnswer().equalsIgnoreCase(answerReq.selectedAnswer().trim());

            QuizAnswer answer = new QuizAnswer();
            answer.setQuestionId(question.getId());
            answer.setSelectedAnswer(answerReq.selectedAnswer().trim().toUpperCase());
            answer.setIsCorrect(correct);

            attempt.getAnswers().add(answer);
            if (correct) {
                score++;
            }
        }

        attempt.setScore(score);
        attempt.setCompleted(true);
        attempt.setSubmittedAt(LocalDateTime.now());

        return quizAttemptRepository.save(attempt);
    }

    public List<QuizAttempt> getQuizResults(Long quizId, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!quiz.getTeacherId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator teacher can view results.");
        }

        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(quizId);
        attempts.sort(Comparator.comparingInt(QuizAttempt::getScore).reversed());
        return attempts;
    }
}
