package com.lumo.backend.exams.controller;

import com.lumo.backend.exams.dto.ExamRequest;
import com.lumo.backend.exams.entity.Exam;
import com.lumo.backend.exams.service.ExamService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping
    public ResponseEntity<Exam> createExam(@RequestBody ExamRequest request) {
        return ResponseEntity.ok(examService.createExam(request));
    }

    @GetMapping
    public ResponseEntity<List<Exam>> getAllExams(@org.springframework.web.bind.annotation.RequestParam(required = false) String name) {
        if (name != null && !name.trim().isEmpty()) {
            return ResponseEntity.ok(examService.getExamsByName(name));
        }
        return ResponseEntity.ok(examService.getAllExams());
    }

    @GetMapping("/school")
    public ResponseEntity<List<Exam>> getSchoolExams() {
        return ResponseEntity.ok(examService.getSchoolExams());
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Exam>> getExamsByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(examService.getExamsByClass(classId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Exam> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamById(id));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<Exam> updateExam(@PathVariable Long id, @RequestBody ExamRequest request) {
        return ResponseEntity.ok(examService.updateExam(id, request));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{examId}/subjects/{subjectId}/results")
    public ResponseEntity<List<com.lumo.backend.exams.entity.ExamResult>> saveExamResults(
            @PathVariable Long examId,
            @PathVariable Long subjectId,
            @RequestBody List<com.lumo.backend.exams.dto.ExamResultRequest> requests) {
        return ResponseEntity.ok(examService.saveExamResults(examId, subjectId, requests));
    }

    @GetMapping("/student/{studentId}/report-card")
    public ResponseEntity<com.lumo.backend.exams.dto.ReportCardResponse> getStudentReportCard(
            @PathVariable String studentId) {
        return ResponseEntity.ok(examService.getStudentReportCard(studentId));
    }

    @PostMapping("/{examId}/publish/class/{classId}")
    public ResponseEntity<java.util.Map<String, Object>> publishExamClassWise(
            @PathVariable Long examId,
            @PathVariable Long classId) {
        int count = examService.publishExamResultsClassWise(examId, classId);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("publishedCount", count);
        response.put("message", "Published exam results for class " + classId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{examId}/publish/overall")
    public ResponseEntity<java.util.Map<String, Object>> publishExamOverall(
            @PathVariable Long examId) {
        int count = examService.publishExamResultsOverall(examId);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("publishedCount", count);
        response.put("message", "Published overall exam results");
        return ResponseEntity.ok(response);
    }
}
