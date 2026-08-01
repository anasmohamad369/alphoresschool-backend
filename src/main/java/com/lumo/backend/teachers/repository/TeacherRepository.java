package com.lumo.backend.teachers.repository;

import com.lumo.backend.teachers.entity.Teacher;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    @Cacheable(value = "teachersByEmail", key = "#emailId")
    Optional<Teacher> findByEmailId(String emailId);

    @Cacheable(value = "teachersByClass", key = "#classTeacher")
    Optional<Teacher> findFirstByClassTeacher(String classTeacher);

    Optional<Teacher> findByMobileNumber(String mobileNumber);
}
