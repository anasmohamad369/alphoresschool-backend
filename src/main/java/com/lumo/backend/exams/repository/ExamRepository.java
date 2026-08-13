package com.lumo.backend.exams.repository;

import com.lumo.backend.exams.entity.Exam;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findBySchoolClassId(Long classId);

    @Query("SELECT e FROM Exam e LEFT JOIN e.schoolClass sc WHERE sc.id = :classId OR sc.id IS NULL")
    List<Exam> findBySchoolClassIdOrSchoolClassIsNull(@Param("classId") Long classId);

    List<Exam> findBySchoolClassIsNull();

    List<Exam> findByExamNameContainingIgnoreCase(String examName);
}

