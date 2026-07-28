package com.lumo.backend.quiz.service;

import com.lumo.backend.quiz.dto.*;
import com.lumo.backend.quiz.entity.*;
import com.lumo.backend.quiz.repository.*;
import com.lumo.backend.teachers.entity.Teacher;
import com.lumo.backend.teachers.repository.TeacherRepository;
import com.lumo.backend.security.JwtService;
import com.lumo.backend.admin.repository.PrincipalRepository;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.students.entity.Student;
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
    private final PrincipalRepository principalRepository;
    private final StudentRepository studentRepository;

    public QuizService(
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            TeacherRepository teacherRepository,
            JwtService jwtService,
            GeminiService geminiService,
            PrincipalRepository principalRepository,
            StudentRepository studentRepository
    ) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.teacherRepository = teacherRepository;
        this.jwtService = jwtService;
        this.geminiService = geminiService;
        this.principalRepository = principalRepository;
        this.studentRepository = studentRepository;
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

    private void getAuthenticatedPrincipal(String authHeader) {
        String token = getHeaderToken(authHeader);
        String emailId = jwtService.extractAdminSubject(token);
        if (emailId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal token.");
        }
        principalRepository.findByEmailId(emailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Principal account not found."));
    }

    public Quiz createQuiz(QuizCreateRequest request, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);

        Quiz quiz = new Quiz();
        quiz.setTitle(request.title());
        quiz.setTopic(request.topic());
        quiz.setDescription(request.description());
        quiz.setClassId(request.classId());
        quiz.setStatus("DRAFT");
        quiz.setTeacherId(teacher.getId());

        return quizRepository.save(quiz);
    }

    public Quiz updateQuiz(Long quizId, QuizCreateRequest request, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!quiz.getTeacherId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator teacher can update this quiz.");
        }

        quiz.setTitle(request.title());
        quiz.setTopic(request.topic());
        quiz.setDescription(request.description());
        quiz.setClassId(request.classId());

        return quizRepository.save(quiz);
    }

    public void deleteQuiz(Long quizId, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!quiz.getTeacherId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator teacher can delete this quiz.");
        }

        quizAttemptRepository.deleteByQuizId(quizId);
        quizRepository.delete(quiz);
    }

    public Quiz getQuizById(Long quizId, String authHeader) {
        String token = getHeaderToken(authHeader);
        String studentId = jwtService.extractStudentSubject(token);
        String teacherEmail = jwtService.extractTeacherSubject(token);
        String adminEmail = jwtService.extractAdminSubject(token);
        if (studentId == null && teacherEmail == null && adminEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token.");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found with id: " + quizId));

        if (studentId != null) {
            Student student = studentRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student not found."));
            if (quiz.getClassId() != null) {
                if (student.getSchoolClass() == null || !quiz.getClassId().equals(student.getSchoolClass().getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This quiz is not assigned to your class.");
                }
            }
        }

        return quiz;
    }

    public List<Quiz> getAllQuizzes(String authHeader) {
        String token = getHeaderToken(authHeader);
        String studentId = jwtService.extractStudentSubject(token);
        String teacherEmail = jwtService.extractTeacherSubject(token);
        String adminEmail = jwtService.extractAdminSubject(token);
        if (studentId == null && teacherEmail == null && adminEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token.");
        }

        if (studentId != null) {
            Student student = studentRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student not found."));
            if (student.getSchoolClass() == null) {
                return new ArrayList<>();
            }
            return quizRepository.findByClassId(student.getSchoolClass().getId());
        }

        return quizRepository.findAll();
    }

    public List<QuestionDTO> generateAIQuestions(QuestionGenerateRequest request, String authHeader) {
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

    public Quiz requestActivation(Long quizId, String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!quiz.getTeacherId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator teacher can request activation for this quiz.");
        }

        if (!"DRAFT".equals(quiz.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only quiz drafts can be requested for activation.");
        }

        quiz.setStatus("PENDING");
        return quizRepository.save(quiz);
    }

    public Quiz startQuiz(Long quizId, String authHeader) {
        getAuthenticatedPrincipal(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        quiz.setStatus("STARTED");
        return quizRepository.save(quiz);
    }

    public Quiz completeQuiz(Long quizId, String authHeader) {
        getAuthenticatedPrincipal(authHeader);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        quiz.setStatus("COMPLETED");
        return quizRepository.save(quiz);
    }

    public List<Quiz> getActiveQuizzes(String authHeader) {
        String token = getHeaderToken(authHeader);
        String studentId = jwtService.extractStudentSubject(token);
        if (studentId != null) {
            Student student = studentRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student not found."));
            if (student.getSchoolClass() == null) {
                return new ArrayList<>();
            }
            return quizRepository.findByStatusAndClassId("STARTED", student.getSchoolClass().getId());
        }

        return quizRepository.findByStatus("STARTED");
    }

    public QuizAttempt registerForQuiz(Long quizId, String authHeader) {
        String studentId = getAuthenticatedStudentId(authHeader);
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student not found."));

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!"STARTED".equals(quiz.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz is not currently active.");
        }

        if (quiz.getClassId() != null) {
            if (student.getSchoolClass() == null || !quiz.getClassId().equals(student.getSchoolClass().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This quiz is not assigned to your class.");
            }
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
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student not found."));

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (!"STARTED".equals(quiz.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz has ended or is not active.");
        }

        if (quiz.getClassId() != null) {
            if (student.getSchoolClass() == null || !quiz.getClassId().equals(student.getSchoolClass().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This quiz is not assigned to your class.");
            }
        }

        QuizAttempt attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId)
                .orElseGet(() -> {
                    QuizAttempt newAttempt = new QuizAttempt();
                    newAttempt.setQuizId(quizId);
                    newAttempt.setStudentId(studentId);
                    newAttempt.setScore(0);
                    newAttempt.setCompleted(false);
                    return quizAttemptRepository.save(newAttempt);
                });

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
        String token = getHeaderToken(authHeader);
        String teacherEmail = jwtService.extractTeacherSubject(token);
        String adminEmail = jwtService.extractAdminSubject(token);

        if (teacherEmail == null && adminEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token.");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));

        if (teacherEmail != null) {
            Teacher teacher = teacherRepository.findByEmailId(teacherEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Teacher account not found."));
            if (!quiz.getTeacherId().equals(teacher.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator teacher or Principal can view results.");
            }
        }

        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(quizId);
        attempts.sort(Comparator.comparingInt(QuizAttempt::getScore).reversed());
        return attempts;
    }
}
