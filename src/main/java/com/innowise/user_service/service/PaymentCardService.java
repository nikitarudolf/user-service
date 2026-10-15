package com.innowise.user_service.service;

import com.innowise.user_service.dto.PaymentCardRequest;
import com.innowise.user_service.dto.PaymentCardResponse;
import com.innowise.user_service.entity.PaymentCard;
import com.innowise.user_service.entity.User;
import com.innowise.user_service.exception.CardLimitExceededException;
import com.innowise.user_service.exception.ResourceAlreadyExistsException;
import com.innowise.user_service.exception.ResourceNotFoundException;
import com.innowise.user_service.mapper.PaymentCardMapper;
import com.innowise.user_service.repository.PaymentCardRepository;
import com.innowise.user_service.repository.UserRepository;
import com.innowise.user_service.repository.specification.PaymentCardSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCardService {

    private static final int MAX_CARDS_PER_USER = 5;

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;

    @Transactional
    public PaymentCardResponse create(Long userId, PaymentCardRequest request) {
        User user = userRepository.findByIdWithCards(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
        if (user.getCards().size() >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException("User with id " + userId + " already has " + MAX_CARDS_PER_USER + " cards");
        }
        if (cardRepository.existsByNumber(request.number())) {
            throw new ResourceAlreadyExistsException("Card with this number already exists");
        }
        PaymentCard card = cardMapper.toEntity(request);
        user.addCard(card);
        userRepository.flush();
        return cardMapper.toResponse(card);
    }

    public PaymentCardResponse getById(Long id) {
        return cardMapper.toResponse(findByIdOrThrow(id));
    }

    public List<PaymentCardResponse> getAllByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User with id " + userId + " not found");
        }
        return cardMapper.toResponseList(cardRepository.findAllByUserId(userId));
    }

    public Page<PaymentCardResponse> getAll(String name, String surname, Pageable pageable) {
        return cardRepository.findAll(PaymentCardSpecifications.ownerHasNameAndSurname(name, surname), pageable)
                .map(cardMapper::toResponse);
    }

    @Transactional
    public PaymentCardResponse update(Long id, PaymentCardRequest request) {
        PaymentCard card = findByIdOrThrow(id);
        if (!card.getNumber().equals(request.number()) && cardRepository.existsByNumber(request.number())) {
            throw new ResourceAlreadyExistsException("Card with this number already exists");
        }
        cardMapper.updateEntity(request, card);
        return cardMapper.toResponse(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        if (cardRepository.updateActiveStatus(id, active) == 0) {
            throw new ResourceNotFoundException("Card with id " + id + " not found");
        }
    }

    @Transactional
    public void delete(Long id) {
        cardRepository.delete(findByIdOrThrow(id));
    }

    private PaymentCard findByIdOrThrow(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card with id " + id + " not found"));
    }
}