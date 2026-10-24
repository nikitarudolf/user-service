package com.innowise.user_service.security;

import com.innowise.user_service.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardAccessChecker {

    private final PaymentCardRepository cardRepository;

    public boolean isOwner(Long cardId, Long userId) {
        return cardRepository.existsByIdAndUserId(cardId, userId);
    }
}
