package com.innowise.user_service.repository;

import com.innowise.user_service.entity.PaymentCard;

import com.innowise.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {

    List<PaymentCard> findAllByUserId(Long userId);

    boolean existsByNumber(String number);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE payment_cards SET active = :active, updated_at = now() WHERE id = :id",
            nativeQuery = true)
    int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);

    Long user(User user);
}
