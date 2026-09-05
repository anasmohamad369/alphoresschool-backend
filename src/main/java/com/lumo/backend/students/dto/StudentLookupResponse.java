package com.lumo.backend.students.dto;

import java.util.List;

public record StudentLookupResponse(
    boolean success,
    String message,
    List<ChildSummaryDto> children
) {}
