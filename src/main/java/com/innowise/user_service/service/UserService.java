package com.innowise.user_service.service;

import com.innowise.user_service.config.CacheConfig;
import com.innowise.user_service.dto.UserRequest;
import com.innowise.user_service.dto.UserResponse;
import com.innowise.user_service.entity.User;
import com.innowise.user_service.exception.ResourceAlreadyExistsException;
import com.innowise.user_service.exception.ResourceNotFoundException;
import com.innowise.user_service.mapper.UserMapper;
import com.innowise.user_service.repository.UserRepository;
import com.innowise.user_service.repository.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(Long id, UserRequest request) {
        if (userRepository.existsById(id)) {
            throw new ResourceAlreadyExistsException("User with id " + id + " already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException("User with email " + request.email() + " already exists");
        }
        User user = userMapper.toEntity(request);
        user.setId(id);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Cacheable(cacheNames = CacheConfig.USERS_CACHE, key = "#id")
    public UserResponse getById(Long id) {
        User user = userRepository.findByIdWithCards(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        return userMapper.toResponse(user);
    }

    public Page<UserResponse> getAll(String name, String surname, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.hasNameAndSurname(name, surname), pageable)
                .map(userMapper::toResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.USERS_CACHE, key = "#id")
    public UserResponse update(Long id, UserRequest request) {
        User user = findByIdOrThrow(id);
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException("User with email " + request.email() + " already exists");
        }
        userMapper.updateEntity(request, user);
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.USERS_CACHE, key = "#id")
    public void setActive(Long id, boolean active) {
        if (userRepository.updateActiveStatus(id, active) == 0) {
            throw new ResourceNotFoundException("User with id " + id + " not found");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.USERS_CACHE, key = "#id")
    public void delete(Long id) {
        userRepository.delete(findByIdOrThrow(id));
    }

    private User findByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }
}