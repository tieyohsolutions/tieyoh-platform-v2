package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.ConnectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {

    Optional<ConnectionRequest> findBySenderIdAndRecipientIdAndStatus(
            Long senderId, Long recipientId, String status);
}
