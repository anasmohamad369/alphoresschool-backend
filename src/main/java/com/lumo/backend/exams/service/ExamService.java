package com.lumo.backend.exams.service;

import com.lumo.backend.classes.entity.SchoolClass;
import com.lumo.backend.classes.repository.SchoolClassRepository;
import com.lumo.backend.exams.dto.ExamRequest;
import com.lumo.backend.exams.entity.Exam;
import com.lumo.backend.exams.repository.ExamRepository;
import com.lumo.backend.exams.repository.ExamResultRepository;
import com.lumo.backend.students.repository.StudentRepository;
import com.lumo.backend.students.entity.Student;
import com.lumo.backend.exams.entity.ExamResult;
import com.lumo.backend.exams.entity.ExamSubject;
import com.lumo.backend.exams.dto.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.lumo.backend.marks.repository.MarkRepository;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final SchoolClassRepository classRepository;
    private final ExamResultRepository examResultRepository;
    private final StudentRepository studentRepository;
    private final MarkRepository markRepository;

    public ExamService(ExamRepository examRepository, SchoolClassRepository classRepository, ExamResultRepository examResultRepository, StudentRepository studentRepository, MarkRepository markRepository) {
        this.examRepository = examRepository;
        this.classRepository = classRepository;
        this.examResultRepository = examResultRepository;
        this.studentRepository = studentRepository;
        this.markRepository = markRepository;
    }

    public Exam createExam(ExamRequest request) {
        SchoolClass sc = null;
        if (request.classId() != null) {
            sc = classRepository.findById(request.classId())
                    .orElseThrow(() -> new IllegalArgumentException("Class not found: " + request.classId()));
        }

        Exam exam = new Exam();
        exam.setSchoolClass(sc);
        exam.setExamName(request.examName());
        exam.setStartDate(request.startDate());
        exam.setEndDate(request.endDate());
        if (request.status() != null) {
            exam.setStatus(request.status());
        } else {
            exam.setStatus("PENDING");
        }

        if (request.subjects() != null) {
            for (com.lumo.backend.exams.dto.ExamSubjectRequest subReq : request.subjects()) {
                com.lumo.backend.exams.entity.ExamSubject subject = new com.lumo.backend.exams.entity.ExamSubject();
                subject.setExam(exam);
                subject.setSubject(subReq.subject());
                subject.setExamDate(subReq.examDate());
                subject.setStartTime(subReq.startTime());
                subject.setEndTime(subReq.endTime());
                subject.setMaxMarks(subReq.maxMarks());
                exam.getSubjects().add(subject);
            }
        }

        return examRepository.save(exam);
    }

    public List<Exam> getExamsByClass(Long classId) {
        return examRepository.findBySchoolClassIdOrSchoolClassIsNull(classId);
    }

    public List<Exam> getSchoolExams() {
        return examRepository.findBySchoolClassIsNull();
    }

    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    public List<Exam> getExamsByName(String name) {
        return examRepository.findByExamNameContainingIgnoreCase(name);
    }

    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Exam not found"));
    }

    public Exam updateExam(Long id, ExamRequest request) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Exam not found"));
        
        SchoolClass sc = null;
        if (request.classId() != null) {
            sc = classRepository.findById(request.classId())
                    .orElseThrow(() -> new IllegalArgumentException("Class not found: " + request.classId()));
        }

        exam.setSchoolClass(sc);
        exam.setExamName(request.examName());
        exam.setStartDate(request.startDate());
        exam.setEndDate(request.endDate());
        if (request.status() != null) {
            exam.setStatus(request.status());
        }

        exam.getSubjects().clear();

        if (request.subjects() != null) {
            for (com.lumo.backend.exams.dto.ExamSubjectRequest subReq : request.subjects()) {
                com.lumo.backend.exams.entity.ExamSubject subject = new com.lumo.backend.exams.entity.ExamSubject();
                subject.setExam(exam);
                subject.setSubject(subReq.subject());
                subject.setExamDate(subReq.examDate());
                subject.setStartTime(subReq.startTime());
                subject.setEndTime(subReq.endTime());
                subject.setMaxMarks(subReq.maxMarks());
                exam.getSubjects().add(subject);
            }
        }

        return examRepository.save(exam);
    }

    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Exam not found");
        }
        examRepository.deleteById(id);
    }

    public List<ExamResult> saveExamResults(Long examId, Long subjectId, List<ExamResultRequest> requests) {
        Exam exam = getExamById(examId);
        ExamSubject targetSubject = exam.getSubjects().stream()
                .filter(s -> s.getId().equals(subjectId))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Exam subject not found"));

        List<ExamResult> savedResults = new java.util.ArrayList<>();
        for (ExamResultRequest req : requests) {
            ExamResult result = examResultRepository.findByStudentIdAndExamSubjectId(req.studentId(), subjectId)
                    .orElse(new ExamResult());
            result.setStudentId(req.studentId());
            result.setExamSubject(targetSubject);
            result.setMarksObtained(req.marksObtained());
            result.setGrade(req.grade());
            result.setRemarks(req.remarks());
            if (result.getPublished() == null) {
                result.setPublished(false);
            }
            savedResults.add(examResultRepository.save(result));
        }
        return savedResults;
    }

    public ReportCardResponse getStudentReportCard(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Student not found"));

        // ONLY fetch published exam results for student view
        List<ExamResult> results = examResultRepository.findByStudentIdAndPublishedTrue(studentId);
        List<ExamReportDto> examReports = new java.util.ArrayList<>();

        if (!results.isEmpty()) {
            // Group results by Exam
            Map<Exam, List<ExamResult>> groupedByExam = results.stream()
                    .collect(Collectors.groupingBy(r -> r.getExamSubject().getExam()));

            for (Map.Entry<Exam, List<ExamResult>> entry : groupedByExam.entrySet()) {
                Exam exam = entry.getKey();
                List<ExamResult> examResults = entry.getValue();

                double totalMarksObtained = 0.0;
                double totalMaxMarks = 0.0;
                List<SubjectMarkDto> subjectMarks = new java.util.ArrayList<>();

                for (ExamResult r : examResults) {
                    double max = r.getExamSubject().getMaxMarks() != null ? r.getExamSubject().getMaxMarks() : 0.0;
                    double obtained = r.getMarksObtained() != null ? r.getMarksObtained() : 0.0;

                    totalMaxMarks += max;
                    totalMarksObtained += obtained;

                    subjectMarks.add(new SubjectMarkDto(
                            r.getExamSubject().getId(),
                            r.getExamSubject().getSubject(),
                            max,
                            obtained,
                            r.getGrade(),
                            r.getRemarks()
                    ));
                }

                double percentage = totalMaxMarks > 0 ? (totalMarksObtained / totalMaxMarks) * 100 : 0.0;

                examReports.add(new ExamReportDto(
                        exam.getId(),
                        exam.getExamName(),
                        subjectMarks,
                        totalMarksObtained,
                        totalMaxMarks,
                        percentage
                ));
            }
        } else {
            // Fetch ONLY published records from marks table
            List<com.lumo.backend.marks.entity.Mark> marks = markRepository.findByStudentIdAndPublishedTrue(studentId);
            if (marks.isEmpty()) {
                marks = markRepository.findByStudentIdAndPublishedTrue(String.valueOf(student.getId()));
            }

            Map<Exam, List<com.lumo.backend.marks.entity.Mark>> groupedByExam = marks.stream()
                    .filter(m -> m.getExam() != null)
                    .collect(Collectors.groupingBy(com.lumo.backend.marks.entity.Mark::getExam));

            for (Map.Entry<Exam, List<com.lumo.backend.marks.entity.Mark>> entry : groupedByExam.entrySet()) {
                Exam exam = entry.getKey();
                List<com.lumo.backend.marks.entity.Mark> examMarks = entry.getValue();

                double totalMarksObtained = 0.0;
                double totalMaxMarks = 0.0;
                List<SubjectMarkDto> subjectMarks = new java.util.ArrayList<>();

                for (com.lumo.backend.marks.entity.Mark m : examMarks) {
                    double max = m.getMaxMarks() != null ? m.getMaxMarks() : 100.0;
                    double obtained = m.getMarksObtained() != null ? m.getMarksObtained() : 0.0;

                    totalMaxMarks += max;
                    totalMarksObtained += obtained;

                    double subjectPercent = max > 0 ? (obtained / max) * 100.0 : 0.0;
                    String subjectGrade = calculateGrade(subjectPercent);

                    subjectMarks.add(new SubjectMarkDto(
                            m.getId(),
                            m.getSubject(),
                            max,
                            obtained,
                            subjectGrade,
                            obtained >= 50.0 ? "Passed" : "Needs Improvement"
                    ));
                }

                double percentage = totalMaxMarks > 0 ? (totalMarksObtained / totalMaxMarks) * 100.0 : 0.0;

                examReports.add(new ExamReportDto(
                        exam.getId(),
                        exam.getExamName(),
                        subjectMarks,
                        totalMarksObtained,
                        totalMaxMarks,
                        percentage
                ));
            }
        }

        String className = student.getSchoolClass() != null ? student.getSchoolClass().getName() : null;
        String sectionName = student.getSection() != null ? student.getSection().getName() : null;
        String studentName = student.getFirstName() + " " + student.getLastName();

        return new ReportCardResponse(
                student.getStudentId(),
                studentName,
                className,
                sectionName,
                examReports
        );
    }

    @org.springframework.transaction.annotation.Transactional
    public int publishExamResultsClassWise(Long examId, Long classId) {
        List<Student> students = studentRepository.findBySchoolClassId(classId);
        List<String> studentIds = new java.util.ArrayList<>();
        for (Student s : students) {
            if (s.getStudentId() != null) studentIds.add(s.getStudentId());
            studentIds.add(String.valueOf(s.getId()));
        }
        if (studentIds.isEmpty()) return 0;

        int publishedCount = 0;
        if (examId != null) {
            publishedCount += examResultRepository.publishByExamIdAndStudentIdIn(examId, studentIds);
            publishedCount += markRepository.publishByExamIdAndStudentIdIn(examId, studentIds);
        } else {
            publishedCount += examResultRepository.publishByStudentIdIn(studentIds);
            publishedCount += markRepository.publishByStudentIdIn(studentIds);
        }
        return publishedCount;
    }

    @org.springframework.transaction.annotation.Transactional
    public int publishExamResultsOverall(Long examId) {
        int publishedCount = 0;
        if (examId != null) {
            publishedCount += examResultRepository.publishByExamId(examId);
            publishedCount += markRepository.publishByExamId(examId);
        } else {
            publishedCount += examResultRepository.publishAll();
            publishedCount += markRepository.publishAll();
        }
        return publishedCount;
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
