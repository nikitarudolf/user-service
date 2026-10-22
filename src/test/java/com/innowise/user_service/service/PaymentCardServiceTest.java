package com.innowise.user_service.service;

import com.innowise.user_service.config.CacheConfig;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper cardMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private PaymentCardService cardService;

    private PaymentCardRequest request;
    private User user;
    private PaymentCard card;
    private PaymentCardResponse response;

    @BeforeEach
    void setUp() {
        request = new PaymentCardRequest("4111111111111111", "IVAN PETROV", LocalDate.of(2030, 12, 31));

        user = new User();
        user.setId(1L);

        card = new PaymentCard();
        card.setId(10L);
        card.setNumber("4111111111111111");
        card.setUser(user);

        response = new PaymentCardResponse(10L, 1L, "4111111111111111", "IVAN PETROV",
                LocalDate.of(2030, 12, 31), true, null, null);
    }

    @Test
    void create_shouldAddCardToUserAndEvictCache() {
        PaymentCard newCard = new PaymentCard();
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(user));
        when(cardRepository.existsByNumber(request.number())).thenReturn(false);
        when(cardMapper.toEntity(request)).thenReturn(newCard);
        when(cardMapper.toResponse(newCard)).thenReturn(response);
        when(cacheManager.getCache(CacheConfig.USERS_CACHE)).thenReturn(cache);

        PaymentCardResponse result = cardService.create(1L, request);

        assertThat(result).isEqualTo(response);
        assertThat(user.getCards()).containsExactly(newCard);
        assertThat(newCard.getUser()).isSameAs(user);
        verify(userRepository).flush();
        verify(cache).evict(1L);
    }

    @Test
    void create_shouldThrow_whenUserNotFound() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.create(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldThrow_whenUserAlreadyHasFiveCards() {
        for (int i = 0; i < 5; i++) {
            user.addCard(new PaymentCard());
        }
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> cardService.create(1L, request))
                .isInstanceOf(CardLimitExceededException.class);
        verify(userRepository, never()).flush();
    }

    @Test
    void create_shouldThrow_whenNumberAlreadyExists() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(user));
        when(cardRepository.existsByNumber(request.number())).thenReturn(true);

        assertThatThrownBy(() -> cardService.create(1L, request))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(userRepository, never()).flush();
    }

    @Test
    void getById_shouldReturnCard_whenExists() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(response);

        assertThat(cardService.getById(10L)).isEqualTo(response);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllByUserId_shouldReturnCards_whenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(cardRepository.findAllByUserId(1L)).thenReturn(List.of(card));
        when(cardMapper.toResponseList(List.of(card))).thenReturn(List.of(response));

        assertThat(cardService.getAllByUserId(1L)).containsExactly(response);
    }

    @Test
    void getAllByUserId_shouldThrow_whenUserNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> cardService.getAllByUserId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(cardRepository, never()).findAllByUserId(anyLong());
    }

    @Test
    void getAll_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cardRepository.findAll(ArgumentMatchers.<Specification<PaymentCard>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(card), pageable, 1));
        when(cardMapper.toResponse(card)).thenReturn(response);

        Page<PaymentCardResponse> result = cardService.getAll(null, "petrov", pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void update_shouldUpdateCardAndEvictCache_whenNumberNotChanged() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cardRepository.saveAndFlush(card)).thenReturn(card);
        when(cardMapper.toResponse(card)).thenReturn(response);
        when(cacheManager.getCache(CacheConfig.USERS_CACHE)).thenReturn(cache);

        PaymentCardResponse result = cardService.update(10L, request);

        assertThat(result).isEqualTo(response);
        verify(cardMapper).updateEntity(request, card);
        verify(cardRepository, never()).existsByNumber(any());
        verify(cache).evict(1L);
    }

    @Test
    void update_shouldThrow_whenNewNumberIsTaken() {
        card.setNumber("5555555555554444");
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cardRepository.existsByNumber(request.number())).thenReturn(true);

        assertThatThrownBy(() -> cardService.update(10L, request))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(cardMapper, never()).updateEntity(any(), any());
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.update(10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setActive_shouldUpdateStatusAndEvictCache_whenCardExists() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cacheManager.getCache(CacheConfig.USERS_CACHE)).thenReturn(cache);

        cardService.setActive(10L, false);

        verify(cardRepository).updateActiveStatus(10L, false);
        verify(cache).evict(1L);
    }

    @Test
    void setActive_shouldThrow_whenNotFound() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.setActive(10L, false))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(cardRepository, never()).updateActiveStatus(anyLong(), anyBoolean());
    }

    @Test
    void delete_shouldDeleteCardAndEvictCache_whenExists() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cacheManager.getCache(CacheConfig.USERS_CACHE)).thenReturn(cache);

        cardService.delete(10L);

        verify(cardRepository).delete(card);
        verify(cache).evict(1L);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.delete(10L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(cardRepository, never()).delete(any(PaymentCard.class));
    }
}
