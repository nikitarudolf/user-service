package com.innowise.user_service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentCardResponse(
        Long id,
        Long userId,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}