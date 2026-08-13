package com.lumo.backend.marks.controller;

import com.lumo.backend.marks.dto.MarkRequest;
import com.lumo.backend.marks.dto.ReportCardResponse;
import com.lumo.backend.marks.entity.Mark;
import com.lumo.backend.marks.service.MarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marks")
public class MarkController {

    private final MarkService markService;

    public MarkController(MarkService markService) {
        this.markService = markService;
    }

    @PostMapping
    public ResponseEntity<Mark> saveMark(@RequestBody MarkRequest request) {
        return ResponseEntity.ok(markService.saveMark(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<java.util.List<Mark>> saveBulkMarks(@RequestBody com.lumo.backend.marks.dto.BulkMarkRequest request) {
        return ResponseEntity.ok(markService.saveBulkMarks(request));
    }

    @GetMapping("/report-card/student/{studentId}/exam/{examId}")
    public ResponseEntity<ReportCardResponse> getReportCard(
            @PathVariable String studentId,
            @PathVariable Long examId) {
        return ResponseEntity.ok(markService.calculateReportCard(studentId, examId));
    }

    @GetMapping("/admin")
    public ResponseEntity<java.util.List<Mark>> getMarksForAdmin(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long examId) {
        return ResponseEntity.ok(markService.getMarksForAdmin(examId));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<Mark> updateMark(
            @PathVariable Long id,
            @RequestBody Mark mark) {
        return ResponseEntity.ok(markService.updateMark(id, mark));
    }

    @PostMapping("/publish/class/{classId}")
    public ResponseEntity<java.util.Map<String, Object>> publishClassWise(
            @PathVariable Long classId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long examId) {
        int updatedCount = markService.publishMarksClassWise(classId, examId);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("publishedCount", updatedCount);
        response.put("message", "Published marks for classId " + classId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/publish/overall")
    public ResponseEntity<java.util.Map<String, Object>> publishOverall(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long examId) {
        int updatedCount = markService.publishMarksOverall(examId);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("publishedCount", updatedCount);
        response.put("message", "Published overall marks successfully");
        return ResponseEntity.ok(response);
    }
}
