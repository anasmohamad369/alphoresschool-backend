package com.lumo.backend.students.service;

import com.lumo.backend.security.JwtService;
import com.lumo.backend.service.FileStorageService;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.teachers.entity.Teacher;
import com.lumo.backend.teachers.repository.TeacherRepository;
import com.lumo.backend.classes.repository.SchoolClassRepository;
import com.lumo.backend.classes.repository.SectionRepository;
import com.lumo.backend.attendance.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private SchoolClassRepository classRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FileStorageService fileStorageService;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                studentRepository,
                teacherRepository,
                classRepository,
                sectionRepository,
                attendanceRepository,
                jwtService,
                passwordEncoder,
                fileStorageService
        );
    }

    @Test
    void deleteStudent_success() {
        Student student = new Student();
        student.setId(109L);
        student.setStudentId("109");
        student.setProfilePhotoUrl("https://api.lumokido.in/uploads/students/photo.jpg");

        Teacher teacher = new Teacher();
        teacher.setId(1L);
        List<Long> studentIds = new ArrayList<>();
        studentIds.add(109L);
        teacher.setStudentIds(studentIds);

        when(studentRepository.findById(109L)).thenReturn(Optional.of(student));
        when(teacherRepository.findAll()).thenReturn(List.of(teacher));

        studentService.deleteStudent("109");

        verify(teacherRepository, times(1)).save(teacher);
        assertFalse(teacher.getStudentIds().contains(109L));
        verify(fileStorageService, times(1)).deleteFile(student.getProfilePhotoUrl());
        verify(studentRepository, times(1)).delete(student);
    }

    @Test
    void deleteStudent_notFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());
        when(studentRepository.findByStudentId("999")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> studentService.deleteStudent("999"));
    }
}
