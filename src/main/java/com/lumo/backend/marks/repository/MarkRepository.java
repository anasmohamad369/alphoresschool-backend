package com.lumo.backend.marks.repository;

import com.lumo.backend.marks.entity.Mark;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByStudentId(String studentId);
    List<Mark> findByStudentIdAndPublishedTrue(String studentId);
    List<Mark> findByStudentIdAndExamId(String studentId, Long examId);
    List<Mark> findByStudentIdAndExamIdAndPublishedTrue(String studentId, Long examId);
    List<Mark> findByExamId(Long examId);

    @Modifying
    @Transactional
    @Query("UPDATE Mark m SET m.published = true WHERE m.exam.id = :examId")
    int publishByExamId(@Param("examId") Long examId);

    @Modifying
    @Transactional
    @Query("UPDATE Mark m SET m.published = true WHERE m.studentId IN :studentIds")
    int publishByStudentIdIn(@Param("studentIds") List<String> studentIds);

    @Modifying
    @Transactional
    @Query("UPDATE Mark m SET m.published = true WHERE m.exam.id = :examId AND m.studentId IN :studentIds")
    int publishByExamIdAndStudentIdIn(@Param("examId") Long examId, @Param("studentIds") List<String> studentIds);

    @Modifying
    @Transactional
    @Query("UPDATE Mark m SET m.published = true")
    int publishAll();
}
