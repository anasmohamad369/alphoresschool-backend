package com.lumo.backend.students.repository;

import com.lumo.backend.students.entity.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByMobileNumber(String mobileNumber);
    List<Student> findAllByMobileNumber(String mobileNumber);
    List<Student> findByStudentClass(String studentClass);
    Page<Student> findByStudentClass(String studentClass, Pageable pageable);
    Optional<Student> findByStudentId(String studentId);
    Optional<Student> findByStudentIdAndMobileNumber(String studentId, String mobileNumber);
    List<Student> findBySchoolClassId(Long classId);
    Page<Student> findBySchoolClassId(Long classId, Pageable pageable);
    List<Student> findBySchoolClassIdAndSectionId(Long classId, Long sectionId);
    Page<Student> findBySchoolClassIdAndSectionId(Long classId, Long sectionId, Pageable pageable);
    int countBySchoolClassId(Long classId);
    int countBySchoolClassIdAndSectionId(Long classId, Long sectionId);
}
