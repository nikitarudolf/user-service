package com.innowise.user_service.mapper;

import com.innowise.user_service.dto.UserRequest;
import com.innowise.user_service.dto.UserResponse;
import com.innowise.user_service.entity.User;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = PaymentCardMapper.class)
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "cards", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRequest request);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(UserRequest request, @MappingTarget User user);
}