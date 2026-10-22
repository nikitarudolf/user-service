package com.innowise.user_service.mapper;

import com.innowise.user_service.dto.PaymentCardRequest;
import com.innowise.user_service.dto.PaymentCardResponse;
import com.innowise.user_service.entity.PaymentCard;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponse toResponse(PaymentCard card);

    List<PaymentCardResponse> toResponseList(List<PaymentCard> cards);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardRequest request);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(PaymentCardRequest request, @MappingTarget PaymentCard card);
}