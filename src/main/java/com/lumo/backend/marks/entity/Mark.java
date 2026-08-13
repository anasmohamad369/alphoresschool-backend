package com.lumo.backend.marks.entity;

import com.lumo.backend.exams.entity.Exam;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "marks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;

    private String subject;
    private Integer marksObtained;
    private Integer maxMarks; // e.g. 100
    private Boolean published = false;

    @jakarta.persistence.Transient
    private String studentName;

    @jakarta.persistence.Transient
    private String className;
}
