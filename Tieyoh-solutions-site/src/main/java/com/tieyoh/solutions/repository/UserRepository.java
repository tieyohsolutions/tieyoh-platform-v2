package com.tieyoh.solutions.repository;

import com.tieyoh.solutions.domain.User;
import com.tieyoh.solutions.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByStatusOrderByCreatedAtDesc(UserStatus status);
    List<User> findAllByOrderByCreatedAtDesc();
}
