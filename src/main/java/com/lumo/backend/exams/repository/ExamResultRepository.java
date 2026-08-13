package com.lumo.backend.exams.repository;

import com.lumo.backend.exams.entity.ExamResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByStudentId(String studentId);
    List<ExamResult> findByStudentIdAndPublishedTrue(String studentId);
    Optional<ExamResult> findByStudentIdAndExamSubjectId(String studentId, Long examSubjectId);
    List<ExamResult> findByExamSubjectId(Long examSubjectId);

    @Modifying
    @Transactional
    @Query("UPDATE ExamResult er SET er.published = true WHERE er.examSubject.exam.id = :examId")
    int publishByExamId(@Param("examId") Long examId);

    @Modifying
    @Transactional
    @Query("UPDATE ExamResult er SET er.published = true WHERE er.studentId IN :studentIds")
    int publishByStudentIdIn(@Param("studentIds") List<String> studentIds);

    @Modifying
    @Transactional
    @Query("UPDATE ExamResult er SET er.published = true WHERE er.examSubject.exam.id = :examId AND er.studentId IN :studentIds")
    int publishByExamIdAndStudentIdIn(@Param("examId") Long examId, @Param("studentIds") List<String> studentIds);

    @Modifying
    @Transactional
    @Query("UPDATE ExamResult er SET er.published = true")
    int publishAll();
}
