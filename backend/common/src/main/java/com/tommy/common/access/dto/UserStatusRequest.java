package com.tommy.common.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserStatusRequest(@NotBlank @Size(max = 30) String userStatusCode) {
}
