package com.lumo.backend.quiz.repository;

import com.lumo.backend.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizId(Long quizId);
    Optional<QuizAttempt> findByQuizIdAndStudentId(Long quizId, String studentId);
}
