package com.tieyoh.api.auth.service;

import com.tieyoh.api.auth.dto.AddFriendRequest;
import com.tieyoh.api.auth.dto.InvitationBatchResponse;
import com.tieyoh.api.auth.dto.InvitationRecipientRequest;
import com.tieyoh.api.auth.dto.InvitationResult;
import com.tieyoh.api.auth.dto.InvitationStatusResult;
import com.tieyoh.api.auth.entity.AppUser;
import com.tieyoh.api.auth.entity.Invitation;
import com.tieyoh.api.auth.repository.AppUserRepository;
import com.tieyoh.api.auth.repository.InvitationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

// Backs "Invite Friends": the multi-contact picker reachable at login or
// from the + button. Each selected contact gets its own, separately
// redeemable invitation -- never one link shared across everyone -- and
// the raw token is only ever handed to the client that requested it; the
// database keeps a SHA-256 hash of the token (plus hashes, not plaintext,
// of the recipient's phone/email) so a leaked database row can't be
// replayed as a working invite and can't be reversed into contact info.
//
// This never sends anything itself: no SMS gateway, no push, no email
// service. The client hands each generated link to the OS share sheet
// (a per-recipient "Share" button, or a batched "Share All" queue) so
// sending is always something the user's own phone/email/messaging app
// does, at the user's initiation -- Tieyoh never pays to send an
// invitation. Paid delivery is reserved for a separate, later
// transactional-notification feature (e.g. "you received a gift").
@Service
public class InvitationService {

    private static final String BASE_URL = "https://tieyoh.com/i/";
    private static final int EXPIRY_DAYS = 30;
    private static final int TOKEN_BYTES = 18; // 144 bits of entropy
    // Static application-level pepper mixed into every hashed phone/email
    // so the hash column isn't a trivial reverse lookup against a plain
    // list of common numbers/addresses. Not a secret-management solution
    // by itself, just cheap extra resistance.
    private static final String PEPPER = "tieyoh-invite-pepper-v1";

    private final InvitationRepository invitationRepository;
    private final AppUserRepository appUserRepository;
    private final FriendService friendService;
    private final SecureRandom secureRandom = new SecureRandom();

    public InvitationService(InvitationRepository invitationRepository,
                              AppUserRepository appUserRepository,
                              FriendService friendService) {
        this.invitationRepository = invitationRepository;
        this.appUserRepository = appUserRepository;
        this.friendService = friendService;
    }

    public InvitationBatchResponse createBatch(String inviterTieyohId, List<InvitationRecipientRequest> recipients) {
        AppUser inviter = appUserRepository.findByTieyohIdIgnoreCase(inviterTieyohId == null ? "" : inviterTieyohId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inviter not found"));
        if (recipients == null || recipients.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one recipient is required");
        }
        List<InvitationResult> results = new ArrayList<>();
        for (InvitationRecipientRequest recipient : recipients) {
            results.add(createOne(inviter, recipient));
        }
        return new InvitationBatchResponse(results);
    }

    private InvitationResult createOne(AppUser inviter, InvitationRecipientRequest recipient) {
        Long inviterUserId = inviter.getId();
        if (recipient.getDisplayName() == null || recipient.getDisplayName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient display name is required");
        }
        String displayName = recipient.getDisplayName().trim();
        String email = ContactNormalizer.normalizeEmail(recipient.getEmail());
        String phone = ContactNormalizer.normalizePhone(recipient.getPhone());
        if (email == null && phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient must have a phone or email");
        }

        // Keep this contact's place in the inviter's circle the same way a
        // one-off invite from the add-people screen already does: ACTIVE
        // right away if that phone/email turns out to belong to a real
        // account, PENDING (greyed out) otherwise. Reuses the existing
        // dedup logic rather than a second copy of it here.
        AddFriendRequest circleEntry = new AddFriendRequest();
        circleEntry.setDisplayName(displayName);
        circleEntry.setPhone(phone);
        circleEntry.setEmail(email);
        friendService.addFriend(inviter.getTieyohId(), circleEntry);

        // If that phone/email already belongs to a real Tieyoh account,
        // they don't need an invitation link -- they're already reachable,
        // and addFriend above already linked them in as ACTIVE.
        AppUser existingAccount = null;
        if (phone != null) {
            existingAccount = appUserRepository.findByPhone(phone).orElse(null);
        }
        if (existingAccount == null && email != null) {
            existingAccount = appUserRepository.findByEmailIgnoreCase(email).orElse(null);
        }
        if (existingAccount != null) {
            return new InvitationResult(displayName, null);
        }

        String emailHash = email == null ? null : hash(email);
        String phoneHash = phone == null ? null : hash(phone);

        // Re-inviting (or "reminding") the same person: supersede whatever
        // link is still open for them from this inviter first, so there's
        // never more than one currently-valid invitation link outstanding
        // for the same recipient. The old token still can't be recovered
        // to resend as-is -- only its hash was ever stored -- so a
        // reminder means a fresh link, with the previous one retired.
        supersedeOpenInvitations(inviterUserId, phoneHash, emailHash);

        Invitation invitation = new Invitation();
        invitation.setInviterUserId(inviterUserId);
        invitation.setRecipientName(displayName);
        invitation.setRecipientEmailHash(emailHash);
        invitation.setRecipientPhoneHash(phoneHash);
        invitation.setStatus("CREATED");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(EXPIRY_DAYS));

        String rawToken = generateToken();
        invitation.setTokenHash(sha256Hex(rawToken));
        invitationRepository.save(invitation);

        return new InvitationResult(displayName, BASE_URL + rawToken);
    }

    private void supersedeOpenInvitations(Long inviterUserId, String phoneHash, String emailHash) {
        List<Invitation> open = new ArrayList<>();
        if (phoneHash != null) {
            open.addAll(invitationRepository.findByInviterUserIdAndRecipientPhoneHashAndStatus(
                    inviterUserId, phoneHash, "CREATED"));
        }
        if (emailHash != null) {
            open.addAll(invitationRepository.findByInviterUserIdAndRecipientEmailHashAndStatus(
                    inviterUserId, emailHash, "CREATED"));
        }
        for (Invitation stale : open) {
            stale.setStatus("SUPERSEDED");
        }
        if (!open.isEmpty()) {
            invitationRepository.saveAll(open);
        }
    }

    // Lets a future web landing page or deep-link handler check a link
    // before showing a signup screen, without ever exposing the inviter's
    // or recipient's contact details -- only the pieces already safe to
    // show (whether it's still good, who it's addressed to by name, and
    // which inviter account it belongs to).
    public InvitationStatusResult resolveToken(String rawToken) {
        Invitation invitation = invitationRepository.findByTokenHash(sha256Hex(rawToken)).orElse(null);
        if (invitation == null) {
            return new InvitationStatusResult(false, "NOT_FOUND", null, null);
        }
        boolean expired = invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now());
        if (expired && "CREATED".equals(invitation.getStatus())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
        }
        boolean valid = "CREATED".equals(invitation.getStatus());
        return new InvitationStatusResult(valid, invitation.getStatus(), invitation.getRecipientName(), invitation.getInviterUserId());
    }

    // Called once the invited person finishes creating their own account,
    // by whatever signup flow reads the token out of the deep link. Marks
    // the invitation redeemed so it can't be used a second time, then
    // links the inviter and the new account directly using the inviter's
    // own account id already sitting on this Invitation row -- no phone/
    // email guessing needed, since the token itself is unambiguous proof
    // of exactly who invited whom. (This used to call only
    // promotePendingConnections(), which re-derived the inviter by
    // matching the new account's phone/email against PENDING circle rows
    // -- that silently failed to link anything whenever the inviter's
    // saved contact info for this person was wrong or outdated, even
    // though the correct inviter was known precisely via this very
    // token.) promotePendingConnections() still runs afterward as a
    // best-effort second pass, since the same new account could also have
    // pre-existing PENDING invites from OTHER people that only a phone/
    // email match can resolve -- this doesn't replace that, it just stops
    // being the only mechanism for the invite that was actually just
    // redeemed.
    public void markAccepted(String rawToken, Long newUserId) {
        Invitation invitation = invitationRepository.findByTokenHash(sha256Hex(rawToken)).orElse(null);
        if (invitation == null || !"CREATED".equals(invitation.getStatus())) {
            return;
        }
        invitation.setStatus("ACCEPTED");
        invitation.setAcceptedUserId(newUserId);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        AppUser newUser = appUserRepository.findById(newUserId).orElse(null);
        if (newUser != null) {
            friendService.linkFromInvitation(invitation.getInviterUserId(), newUser);
            friendService.promotePendingConnections(newUser);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String normalized) {
        return sha256Hex(PEPPER + ":" + normalized);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

}
