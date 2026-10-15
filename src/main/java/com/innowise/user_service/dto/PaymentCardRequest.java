package com.innowise.user_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PaymentCardRequest(
        @NotBlank @Pattern(regexp = "\\d{13,19}", message = "must contain 13 to 19 digits") String number,
        @NotBlank @Size(max = 200) String holder,
        @NotNull @Future LocalDate expirationDate
) {
}