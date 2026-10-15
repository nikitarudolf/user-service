package com.innowise.user_service.repository.specification;

import com.innowise.user_service.entity.PaymentCard;
import com.innowise.user_service.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PaymentCardSpecifications {

    private PaymentCardSpecifications() {
    }

    public static Specification<PaymentCard> ownerHasNameAndSurname(String name, String surname) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if ((name != null && !name.isBlank()) || (surname != null && !surname.isBlank())) {
                Join<PaymentCard, User> user = root.join("user");
                if (name != null && !name.isBlank()) {
                    predicates.add(cb.like(cb.lower(user.get("name")), "%" + name.toLowerCase() + "%"));
                }
                if (surname != null && !surname.isBlank()) {
                    predicates.add(cb.like(cb.lower(user.get("surname")), "%" + surname.toLowerCase() + "%"));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}