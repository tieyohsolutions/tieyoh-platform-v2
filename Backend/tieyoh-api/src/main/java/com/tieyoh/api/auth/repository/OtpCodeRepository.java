package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String contact, String purpose);

    boolean existsByContactAndPurposeAndConsumedAtIsNotNullAndConsumedAtAfter(
            String contact, String purpose, LocalDateTime after);
}
