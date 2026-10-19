package com.tommy.common.org.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record DeptCloseRequest(LocalDate effectiveDate, @Size(max = 500) String changeReason) {
}
