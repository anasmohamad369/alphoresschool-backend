package com.lumo.backend.marks.service;

import com.lumo.backend.exams.entity.Exam;
import com.lumo.backend.exams.repository.ExamRepository;
import com.lumo.backend.marks.dto.MarkRequest;
import com.lumo.backend.marks.dto.ReportCardResponse;
import com.lumo.backend.marks.dto.SubjectMarkResponse;
import com.lumo.backend.marks.entity.Mark;
import com.lumo.backend.marks.repository.MarkRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarkService {

    private final MarkRepository markRepository;
    private final ExamRepository examRepository;
    private final com.lumo.backend.students.repository.StudentRepository studentRepository;

    public MarkService(MarkRepository markRepository, ExamRepository examRepository, com.lumo.backend.students.repository.StudentRepository studentRepository) {
        this.markRepository = markRepository;
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
    }

    public Mark saveMark(MarkRequest request) {
        Exam exam = examRepository.findById(request.examId())
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + request.examId()));

        Mark mark = new Mark();
        mark.setStudentId(request.studentId());
        mark.setExam(exam);
        mark.setSubject(request.subject());
        mark.setMarksObtained(request.marksObtained());
        mark.setMaxMarks(request.maxMarks() != null ? request.maxMarks() : 100);
        mark.setPublished(false); // Saved as draft by default

        return markRepository.save(mark);
    }

    @org.springframework.transaction.annotation.Transactional
    public List<Mark> saveBulkMarks(com.lumo.backend.marks.dto.BulkMarkRequest request) {
        Exam exam = examRepository.findById(request.examId())
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + request.examId()));

        List<Mark> existingMarks = markRepository.findByStudentIdAndExamId(request.studentId(), request.examId());
        List<Mark> savedMarks = new java.util.ArrayList<>();

        for (com.lumo.backend.marks.dto.SubjectMarkInput input : request.marks()) {
            Mark mark = existingMarks.stream()
                    .filter(m -> m.getSubject().equalsIgnoreCase(input.subject()))
                    .findFirst()
                    .orElse(new Mark());

            mark.setStudentId(request.studentId());
            mark.setExam(exam);
            mark.setSubject(input.subject());
            mark.setMarksObtained(input.marksObtained());
            if (mark.getPublished() == null) {
                mark.setPublished(false);
            }

            Integer resolvedMaxMarks = exam.getSubjects().stream()
                    .filter(s -> s.getSubject().equalsIgnoreCase(input.subject()))
                    .map(com.lumo.backend.exams.entity.ExamSubject::getMaxMarks)
                    .findFirst()
                    .orElse(100);

            mark.setMaxMarks(resolvedMaxMarks != null ? resolvedMaxMarks : 100);
            savedMarks.add(markRepository.save(mark));
        }

        return savedMarks;
    }

    public ReportCardResponse calculateReportCard(String studentId, Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        // Fetch ONLY published marks for student report card
        List<Mark> marksList = markRepository.findByStudentIdAndExamIdAndPublishedTrue(studentId, examId);

        List<SubjectMarkResponse> subjectMarks = new ArrayList<>();
        int totalObtained = 0;
        int totalMax = 0;

        for (Mark m : marksList) {
            subjectMarks.add(new SubjectMarkResponse(m.getSubject(), m.getMarksObtained(), m.getMaxMarks()));
            totalObtained += m.getMarksObtained();
            totalMax += m.getMaxMarks();
        }

        double percentage = 0.0;
        if (totalMax > 0) {
            percentage = ((double) totalObtained / totalMax) * 100.0;
        }

        String grade = calculateGrade(percentage);

        return new ReportCardResponse(
                studentId,
                exam.getExamName(),
                subjectMarks,
                totalObtained,
                totalMax,
                percentage,
                grade
        );
    }

    @org.springframework.transaction.annotation.Transactional
    public int publishMarksClassWise(Long classId, Long examId) {
        List<com.lumo.backend.students.entity.Student> students = studentRepository.findBySchoolClassId(classId);
        List<String> studentIds = new java.util.ArrayList<>();
        for (com.lumo.backend.students.entity.Student s : students) {
            if (s.getStudentId() != null) studentIds.add(s.getStudentId());
            studentIds.add(String.valueOf(s.getId()));
        }
        if (studentIds.isEmpty()) return 0;

        if (examId != null) {
            return markRepository.publishByExamIdAndStudentIdIn(examId, studentIds);
        } else {
            return markRepository.publishByStudentIdIn(studentIds);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public int publishMarksOverall(Long examId) {
        if (examId != null) {
            return markRepository.publishByExamId(examId);
        } else {
            return markRepository.publishAll();
        }
    }

    public List<Mark> getMarksForAdmin(Long examId) {
        if (examId != null) {
            return markRepository.findByExamId(examId);
        }
        return markRepository.findAll();
    }

    public Mark updateMark(Long id, Mark updated) {
        Mark existing = markRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mark not found with id: " + id));
        if (updated.getMarksObtained() != null) {
            existing.setMarksObtained(updated.getMarksObtained());
        }
        if (updated.getMaxMarks() != null) {
            existing.setMaxMarks(updated.getMaxMarks());
        }
        if (updated.getPublished() != null) {
            existing.setPublished(updated.getPublished());
        }
        return markRepository.save(existing);
    }

    private String calculateGrade(double percentage) {
        if (percentage >= 90.0) return "A+";
        if (percentage >= 80.0) return "A";
        if (percentage >= 70.0) return "B";
        if (percentage >= 60.0) return "C";
        if (percentage >= 50.0) return "D";
        return "F";
    }
}
