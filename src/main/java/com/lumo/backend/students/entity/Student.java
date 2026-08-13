package com.lumo.backend.students.entity;

import com.lumo.backend.classes.entity.SchoolClass;
import com.lumo.backend.classes.entity.Section;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String studentId;

    private String password;
    private String status;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne
    @JoinColumn(name = "section_id")
    private Section section;

    private String firstName;
    private String lastName;
    private String middleName;
    private String mobileNumber;
    private String fatherName;
    private String motherName;
    private String dateOfBirth;
    private String gender;

    @Column(name = "student_class")
    private String studentClass;  // kept for backwards compatibility matching teacher.classTeacher

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Embedded
    private FeeDetails feeDetails;
}