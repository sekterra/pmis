package com.tommy.common.org.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record EndAssignmentRequest(LocalDate endDate, @Size(max = 30) String assignmentStatusCode) {
}
