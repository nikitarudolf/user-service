package com.innowise.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserRequest(
        @NotBlank @Size(max = 20) String name,
        @NotBlank @Size(max = 100) String surname,
        @Past LocalDate birthDate,
        @NotBlank @Email @Size(max = 255) String email
) {
}