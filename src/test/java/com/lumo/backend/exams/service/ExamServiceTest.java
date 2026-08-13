package com.lumo.backend.exams.service;

import com.lumo.backend.classes.entity.SchoolClass;
import com.lumo.backend.classes.repository.SchoolClassRepository;
import com.lumo.backend.exams.dto.ExamRequest;
import com.lumo.backend.exams.entity.Exam;
import com.lumo.backend.exams.repository.ExamRepository;
import com.lumo.backend.exams.repository.ExamResultRepository;
import com.lumo.backend.marks.repository.MarkRepository;
import com.lumo.backend.students.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExamServiceTest {

    private ExamRepository examRepository;
    private SchoolClassRepository classRepository;
    private ExamResultRepository examResultRepository;
    private StudentRepository studentRepository;
    private MarkRepository markRepository;
    private ExamService examService;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        classRepository = mock(SchoolClassRepository.class);
        examResultRepository = mock(ExamResultRepository.class);
        studentRepository = mock(StudentRepository.class);
        markRepository = mock(MarkRepository.class);

        examService = new ExamService(
                examRepository,
                classRepository,
                examResultRepository,
                studentRepository,
                markRepository
        );
    }

    @Test
    void testCreateExam() {
        SchoolClass sc = new SchoolClass();
        sc.setId(1L);
        sc.setName("Class 10");

        when(classRepository.findById(1L)).thenReturn(Optional.of(sc));

        Exam exam = new Exam();
        exam.setId(100L);
        exam.setExamName("Mid-Term");
        exam.setSchoolClass(sc);

        when(examRepository.save(any(Exam.class))).thenReturn(exam);

        ExamRequest req = new ExamRequest(1L, "Mid-Term", LocalDate.now(), LocalDate.now().plusDays(5), "PENDING", null);
        Exam created = examService.createExam(req);

        assertNotNull(created);
        assertEquals("Mid-Term", created.getExamName());
        assertEquals(1L, created.getSchoolClass().getId());
    }

    @Test
    void testGetExamsByClass() {
        Exam classExam = new Exam();
        classExam.setId(101L);
        classExam.setExamName("Class 10 Math Exam");

        Exam schoolExam = new Exam();
        schoolExam.setId(102L);
        schoolExam.setExamName("Annual School Sports & General Exam");

        when(examRepository.findBySchoolClassIdOrSchoolClassIsNull(1L)).thenReturn(List.of(classExam, schoolExam));

        List<Exam> exams = examService.getExamsByClass(1L);
        assertEquals(2, exams.size());
    }
}
