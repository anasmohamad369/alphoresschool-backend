package com.lumo.backend.teachers.dto;

import java.util.ArrayList;

public record TeacherAdd(
    boolean success,
    String emailId,
    String name,
    String mobileNumber,
    String classTeacher,
    String dateOfBirth,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.lumo.backend.config.CommaSeparatedStringToListDeserializer.class)
    ArrayList<String> classes,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.lumo.backend.config.CommaSeparatedStringToListDeserializer.class)
    ArrayList<String> subjects
) {
}
