package com.innowise.user_service.service;

import com.innowise.user_service.dto.UserRequest;
import com.innowise.user_service.dto.UserResponse;
import com.innowise.user_service.entity.User;
import com.innowise.user_service.exception.ResourceAlreadyExistsException;
import com.innowise.user_service.exception.ResourceNotFoundException;
import com.innowise.user_service.mapper.UserMapper;
import com.innowise.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UserRequest request;
    private User user;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        request = new UserRequest("Ivan", "Petrov", LocalDate.of(2000, 5, 10), "ivan@example.com");

        user = new User();
        user.setId(1L);
        user.setName("Ivan");
        user.setSurname("Petrov");
        user.setEmail("ivan@example.com");

        response = new UserResponse(1L, "Ivan", "Petrov", LocalDate.of(2000, 5, 10),
                "ivan@example.com", true, null, null, List.of());
    }

    @Test
    void create_shouldSaveUser_whenEmailIsFree() {
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.create(1L, request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).save(user);
    }

    @Test
    void create_shouldThrow_whenProfileAlreadyExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.create(1L, request))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.create(1L, request))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnUser_whenExists() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        assertThat(userService.getById(1L)).isEqualTo(response);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(userRepository.findByIdWithCards(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.getAll("iv", null, pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void update_shouldUpdateUser_whenEmailNotChanged() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.update(1L, request);

        assertThat(result).isEqualTo(response);
        verify(userMapper).updateEntity(request, user);
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void update_shouldThrow_whenNewEmailIsTaken() {
        user.setEmail("old@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(userMapper, never()).updateEntity(any(), any());
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setActive_shouldUpdateStatus_whenUserExists() {
        when(userRepository.updateActiveStatus(1L, false)).thenReturn(1);

        userService.setActive(1L, false);

        verify(userRepository).updateActiveStatus(1L, false);
    }

    @Test
    void setActive_shouldThrow_whenNotFound() {
        when(userRepository.updateActiveStatus(1L, false)).thenReturn(0);

        assertThatThrownBy(() -> userService.setActive(1L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldDeleteUser_whenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userRepository, never()).delete(any(User.class));
    }
}
