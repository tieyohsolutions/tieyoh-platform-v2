package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    // Used to supersede an inviter's earlier still-open invitation to the
    // same person before issuing a fresh one, so a repeat "invite them
    // again" / reminder share never leaves more than one currently-valid
    // link outstanding for the same recipient.
    List<Invitation> findByInviterUserIdAndRecipientPhoneHashAndStatus(
            Long inviterUserId, String recipientPhoneHash, String status);

    List<Invitation> findByInviterUserIdAndRecipientEmailHashAndStatus(
            Long inviterUserId, String recipientEmailHash, String status);
}
