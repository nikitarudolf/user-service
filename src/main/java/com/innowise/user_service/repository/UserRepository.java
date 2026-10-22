package com.innowise.user_service.repository;

import com.innowise.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("select u from User u left join fetch u.cards where u.id = :id")
    Optional<User> findByIdWithCards(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.active = :active, u.updatedAt = CURRENT_TIMESTAMP where u.id = :id")
    int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}