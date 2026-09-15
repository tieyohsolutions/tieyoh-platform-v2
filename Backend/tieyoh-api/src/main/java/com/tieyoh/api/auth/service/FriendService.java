package com.tieyoh.api.auth.service;

import com.tieyoh.api.auth.dto.AddFriendRequest;
import com.tieyoh.api.auth.dto.ContactMatchResult;
import com.tieyoh.api.auth.dto.InvitationRecipientRequest;
import com.tieyoh.api.auth.dto.FeedPersonResult;
import com.tieyoh.api.auth.dto.FriendResult;
import com.tieyoh.api.auth.entity.AppUser;
import com.tieyoh.api.auth.entity.ConnectionRequest;
import com.tieyoh.api.auth.entity.FriendConnection;
import com.tieyoh.api.auth.repository.AppUserRepository;
import com.tieyoh.api.auth.repository.ConnectionRequestRepository;
import com.tieyoh.api.auth.repository.FriendConnectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Backs a user's "circle" -- the persistent list shown behind the add-people
// icon and the send-gift flow, as opposed to UserProfileService's one-off,
// unsaved search results. A circle entry is ACTIVE (linked to a real Tieyoh
// account, full access to gift them), PENDING (invited by phone or email,
// greyed out until they join), or PENDING_APPROVAL (they're already a real
// account, but haven't approved joining this circle yet -- see below).
//
// The owner of a circle is identified by their Tieyoh @t- id
// (FriendConnection.ownerTieyohId), not an internal numeric id -- every
// owner is, by definition, a completed (ACTIVE) account with a tieyoh id by
// the time any screen here is reachable. Most methods below still accept a
// numeric userId at their public boundary where an existing feature (the
// feed's network view, connection requests) already works in numeric ids
// end to end -- they resolve that user's own tieyoh id internally before
// touching FriendConnection rows.
//
// Adding someone who already has a real account is mutual: they end up in
// the adder's circle AND the adder ends up in theirs. Whether that happens
// immediately or needs their say-so first depends on their own
// AppUser.autoAcceptCircleInvites setting (a Profile toggle, default true).
// When it's false, the adder's side is created as PENDING_APPROVAL and the
// other person gets a notification -- reusing the same ConnectionRequest
// mechanism already built for second-degree "connect" taps in the feed,
// rather than a second parallel accept/decline system. Accepting it (via
// the existing ConnectionRequestService.respond) calls connectMutually,
// which finds that exact PENDING_APPROVAL row (by owner tID + contact's
// numeric id) and flips it to ACTIVE, plus links the reverse side.
@Service
public class FriendService {

    private final FriendConnectionRepository friendConnectionRepository;
    private final AppUserRepository appUserRepository;
    private final ConnectionRequestRepository connectionRequestRepository;
    private final NotificationService notificationService;

    public FriendService(FriendConnectionRepository friendConnectionRepository,
                          AppUserRepository appUserRepository,
                          ConnectionRequestRepository connectionRequestRepository,
                          NotificationService notificationService) {
        this.friendConnectionRepository = friendConnectionRepository;
        this.appUserRepository = appUserRepository;
        this.connectionRequestRepository = connectionRequestRepository;
        this.notificationService = notificationService;
    }

    // Read-only classification of a batch of phone-book contacts against
    // real Tieyoh accounts -- unlike addFriend/InvitationService.createBatch,
    // this writes nothing (no circle rows, no invitations, no
    // notifications). Backs the "People you may know" onboarding screen,
    // which needs to know who's already on Tieyoh before the user has
    // tapped Connect or Invite on anything -- those taps are what actually
    // call addFriend / the invitation batch endpoint, same as everywhere
    // else in the app.
    public List<ContactMatchResult> checkContacts(String ownerTieyohId, List<InvitationRecipientRequest> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return new ArrayList<>();
        }

        // The viewer's own ACTIVE circle -- reused below for the "mutual
        // friends" count on each match, the same set getNetwork() already
        // traverses for the feed's second-degree section.
        Set<Long> viewerCircle = friendConnectionRepository.findByOwnerTieyohIdOrderByDisplayNameAsc(ownerTieyohId)
                .stream()
                .filter(fc -> "ACTIVE".equals(fc.getStatus()) && fc.getContactUserId() != null)
                .map(FriendConnection::getContactUserId)
                .collect(Collectors.toSet());

        List<ContactMatchResult> results = new ArrayList<>();
        for (InvitationRecipientRequest contact : contacts) {
            String phone = ContactNormalizer.normalizePhone(contact.getPhone());
            String email = ContactNormalizer.normalizeEmail(contact.getEmail());

            AppUser match = null;
            if (phone != null) {
                match = appUserRepository.findByPhone(phone).filter(u -> "ACTIVE".equals(u.getStatus())).orElse(null);
            }
            if (match == null && email != null) {
                match = appUserRepository.findByEmailIgnoreCase(email).filter(u -> "ACTIVE".equals(u.getStatus())).orElse(null);
            }
            // A contact whose phone/email happens to resolve back to the
            // viewer themselves (their own number saved under "Me", etc.)
            // isn't a real match to show.
            if (match != null && ownerTieyohId.equalsIgnoreCase(match.getTieyohId())) {
                match = null;
            }

            ContactMatchResult result = new ContactMatchResult();
            result.setDisplayName(contact.getDisplayName());
            result.setPhone(contact.getPhone());
            result.setEmail(contact.getEmail());

            if (match != null) {
                result.setMatchedUserId(match.getId());
                result.setMatchedTieyohId(match.getTieyohId());
                result.setMatchedProfilePictureBase64(match.getProfilePictureBase64());

                Set<Long> matchCircle = friendConnectionRepository.findByOwnerTieyohIdOrderByDisplayNameAsc(match.getTieyohId())
                        .stream()
                        .filter(fc -> "ACTIVE".equals(fc.getStatus()) && fc.getContactUserId() != null)
                        .map(FriendConnection::getContactUserId)
                        .collect(Collectors.toSet());
                matchCircle.retainAll(viewerCircle);
                result.setMutualFriendsCount(matchCircle.size());
            }

            results.add(result);
        }
        return results;
    }

    public List<FriendResult> getFriends(String ownerTieyohId) {
        return friendConnectionRepository.findByOwnerTieyohIdOrderByDisplayNameAsc(ownerTieyohId)
                .stream()
                .map(this::toResult)
                .collect(Collectors.toList());
    }

    // Backs the feed's network section: the viewer's own active circle,
    // plus their circle's own active circles (second-degree / "friends of
    // friends"), each tagged with which one it is so the client knows
    // whether tapping someone should start a gift (DIRECT) or send a
    // connection request (SECOND_DEGREE). Public signature stays a numeric
    // userId (the feed/notifications stack all works in numeric ids) --
    // this resolves that user's own tieyoh id internally to query circles.
    public List<FeedPersonResult> getNetwork(Long userId) {
        AppUser viewer = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<FriendConnection> direct = friendConnectionRepository.findByOwnerTieyohIdOrderByDisplayNameAsc(viewer.getTieyohId())
                .stream()
                .filter(fc -> "ACTIVE".equals(fc.getStatus()) && fc.getContactTieyohId() != null)
                .collect(Collectors.toList());

        Set<Long> directIds = direct.stream().map(FriendConnection::getContactUserId).collect(Collectors.toSet());

        Map<Long, FeedPersonResult> byUserId = new LinkedHashMap<>();
        Map<Long, AppUser> directUsers = new LinkedHashMap<>();
        for (FriendConnection fc : direct) {
            AppUser u = appUserRepository.findById(fc.getContactUserId()).orElse(null);
            if (u == null) {
                continue;
            }
            directUsers.put(u.getId(), u);
            byUserId.put(u.getId(), new FeedPersonResult(
                    u.getId(), u.getDisplayName(), u.getTieyohId(), u.getProfilePictureBase64(),
                    "DIRECT", null));
        }

        for (Long directId : directIds) {
            AppUser directUser = directUsers.get(directId);
            if (directUser == null || directUser.getTieyohId() == null) {
                continue;
            }
            List<FriendConnection> theirs = friendConnectionRepository.findByOwnerTieyohIdOrderByDisplayNameAsc(directUser.getTieyohId())
                    .stream()
                    .filter(fc -> "ACTIVE".equals(fc.getStatus()) && fc.getContactTieyohId() != null)
                    .collect(Collectors.toList());

            for (FriendConnection fc : theirs) {
                Long candidateId = fc.getContactUserId();
                if (candidateId.equals(userId) || directIds.contains(candidateId) || byUserId.containsKey(candidateId)) {
                    continue;
                }
                AppUser u = appUserRepository.findById(candidateId).orElse(null);
                if (u == null) {
                    continue;
                }
                byUserId.put(u.getId(), new FeedPersonResult(
                        u.getId(), u.getDisplayName(), u.getTieyohId(), u.getProfilePictureBase64(),
                        "SECOND_DEGREE", connectionRequestStatusBetween(userId, candidateId)));
            }
        }

        return new ArrayList<>(byUserId.values());
    }

    private String connectionRequestStatusBetween(Long userId, Long otherId) {
        if (connectionRequestRepository.findBySenderIdAndRecipientIdAndStatus(userId, otherId, "PENDING").isPresent()) {
            return "PENDING_SENT";
        }
        if (connectionRequestRepository.findBySenderIdAndRecipientIdAndStatus(otherId, userId, "PENDING").isPresent()) {
            return "PENDING_RECEIVED";
        }
        return null;
    }

    // Called when a connection request is accepted -- links both accounts
    // as ACTIVE circle members of each other, the same shape as if each
    // had separately added the other via the search-and-invite flow. Takes
    // numeric ids (ConnectionRequest, its caller, works in numeric ids
    // end to end) and resolves each side's tieyoh id internally. Also the
    // mechanism that resolves a circle-invite approval request once
    // accepted -- see the class comment above.
    public void connectMutually(Long userIdA, Long userIdB) {
        AppUser userA = appUserRepository.findById(userIdA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        AppUser userB = appUserRepository.findById(userIdB)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        upsertActive(userA.getTieyohId(), userB);
        upsertActive(userB.getTieyohId(), userA);
    }

    // Called when a circle-invite approval request is declined -- if the
    // requester's circle still has the PENDING_APPROVAL placeholder created
    // when the request was sent, remove it; there's nothing pending to show
    // any more.
    public void cancelPendingApproval(Long requesterUserId, Long contactUserId) {
        AppUser requester = appUserRepository.findById(requesterUserId).orElse(null);
        if (requester == null || requester.getTieyohId() == null) {
            return;
        }
        friendConnectionRepository.findByOwnerTieyohIdAndContactUserId(requester.getTieyohId(), contactUserId)
                .filter(fc -> "PENDING_APPROVAL".equals(fc.getStatus()))
                .ifPresent(friendConnectionRepository::delete);
    }

    public FriendResult addFriend(String ownerTieyohId, AddFriendRequest request) {
        // Path 1: adding someone already found via the Tieyoh user search --
        // they have a real account. The search itself already excludes
        // incomplete (PENDING_PROFILE) accounts, but this is checked again
        // here defensively so a stale client-side result can't add someone
        // who hasn't finished setting up their @t id yet.
        if (request.getContactUserId() != null) {
            AppUser contact = appUserRepository.findById(request.getContactUserId())
                    .filter(u -> "ACTIVE".equals(u.getStatus()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            return toResult(linkExistingAccount(ownerTieyohId, contact));
        }

        String displayName = request.getDisplayName() == null || request.getDisplayName().isBlank()
                ? "Unknown"
                : request.getDisplayName().trim();
        // Normalized so the same contact is recognized as the same person
        // no matter which flow or screen the raw phone/email came from --
        // required for the dedup checks below to actually catch a repeat.
        String phone = ContactNormalizer.normalizePhone(request.getPhone());
        String email = ContactNormalizer.normalizeEmail(request.getEmail());

        // Path 2: a raw phone/email invite -- but if that phone or email
        // already belongs to a real Tieyoh account, resolve to that
        // account (mutual/approval, same as path 1) instead of creating a
        // dangling pending row. Only an account that's actually finished
        // profile setup (ACTIVE) counts here -- otherwise inviting a
        // phone/email that only has a half-signed-up (PENDING_PROFILE)
        // account behind it would jump straight to a joined-looking circle
        // entry for someone who, as far as anyone else can tell, was never
        // really a Tieyoh user.
        AppUser existingAccount = null;
        if (phone != null) {
            existingAccount = appUserRepository.findByPhone(phone)
                    .filter(u -> "ACTIVE".equals(u.getStatus()))
                    .orElse(null);
        }
        if (existingAccount == null && email != null) {
            existingAccount = appUserRepository.findByEmailIgnoreCase(email)
                    .filter(u -> "ACTIVE".equals(u.getStatus()))
                    .orElse(null);
        }
        if (existingAccount != null) {
            return toResult(linkExistingAccount(ownerTieyohId, existingAccount));
        }

        // This phone or email is already a circle entry for this owner --
        // from an earlier single invite, an earlier "Invite Friends"
        // batch, or simply re-selecting and re-sharing the same contact
        // again. Update that row instead of inserting a duplicate; a
        // "reminder" share is still just a share, not a second person.
        FriendConnection existing = null;
        if (phone != null) {
            existing = friendConnectionRepository.findByOwnerTieyohIdAndPhone(ownerTieyohId, phone).orElse(null);
        }
        if (existing == null && email != null) {
            existing = friendConnectionRepository.findByOwnerTieyohIdAndEmailIgnoreCase(ownerTieyohId, email).orElse(null);
        }
        if (existing != null) {
            existing.setDisplayName(displayName);
            if (phone != null) {
                existing.setPhone(phone);
            }
            if (email != null) {
                existing.setEmail(email);
            }
            return toResult(friendConnectionRepository.save(existing));
        }

        FriendConnection fc = new FriendConnection();
        fc.setOwnerTieyohId(ownerTieyohId);
        fc.setDisplayName(displayName);
        fc.setPhone(phone);
        fc.setEmail(email);
        fc.setStatus("PENDING");
        return toResult(friendConnectionRepository.save(fc));
    }

    // Called once an invitation token is redeemed by a brand-new signup --
    // links the inviter and the new account directly using the inviter's
    // own numeric id (which the Invitation record already carries
    // unambiguously), rather than the fragile phone/email re-matching
    // promotePendingConnections() relies on. Reuses linkExistingAccount's
    // existing auto-accept / PENDING_APPROVAL branching unchanged -- the
    // inviter is just "the owner adding a contact" in that method's terms,
    // same as any other add-to-circle action.
    public void linkFromInvitation(Long inviterUserId, AppUser newUser) {
        AppUser inviter = appUserRepository.findById(inviterUserId).orElse(null);
        if (inviter == null || inviter.getTieyohId() == null) {
            return;
        }
        linkExistingAccount(inviter.getTieyohId(), newUser);
    }

    // Adding an existing account to a circle: mutual and immediate if the
    // contact auto-accepts (the default), otherwise a PENDING_APPROVAL
    // placeholder plus a request they have to approve first.
    private FriendConnection linkExistingAccount(String ownerTieyohId, AppUser contact) {
        if (Boolean.TRUE.equals(contact.getAutoAcceptCircleInvites())) {
            FriendConnection ownerSide = upsertActive(ownerTieyohId, contact);
            AppUser owner = appUserRepository.findByTieyohIdIgnoreCase(ownerTieyohId).orElse(null);
            if (owner != null) {
                upsertActive(contact.getTieyohId(), owner);
                notifyAddedToCircle(owner, contact);
            }
            return ownerSide;
        }
        return requestApproval(ownerTieyohId, contact);
    }

    private FriendConnection requestApproval(String ownerTieyohId, AppUser contact) {
        FriendConnection existing = friendConnectionRepository
                .findByOwnerTieyohIdAndContactUserId(ownerTieyohId, contact.getId())
                .orElse(null);
        if (existing != null && "ACTIVE".equals(existing.getStatus())) {
            // Already fully connected -- nothing to request.
            return existing;
        }

        AppUser owner = appUserRepository.findByTieyohIdIgnoreCase(ownerTieyohId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        FriendConnection fc = existing != null ? existing : new FriendConnection();
        fc.setOwnerTieyohId(ownerTieyohId);
        fc.setContactUserId(contact.getId());
        fc.setDisplayName(contact.getDisplayName());
        fc.setContactTieyohId(contact.getTieyohId());
        fc.setPhone(contact.getPhone());
        fc.setEmail(contact.getEmail());
        fc.setStatus("PENDING_APPROVAL");
        FriendConnection saved = friendConnectionRepository.save(fc);

        sendApprovalRequest(owner, contact);
        return saved;
    }

    // Auto-accept case: the two accounts are already fully, mutually
    // linked by the time this runs, so there's nothing to approve -- this
    // is a plain informational notification, not a ConnectionRequest, and
    // NotificationsScreen already renders any type it doesn't specifically
    // recognize as accept/decline buttons as plain text.
    private void notifyAddedToCircle(AppUser owner, AppUser contact) {
        notificationService.notify(
                contact.getId(),
                "ADDED_TO_CIRCLE",
                owner.getId(),
                owner.getDisplayName(),
                owner.getDisplayName() + " added you to their circle.",
                null,
                null);
    }

    // A PENDING ConnectionRequest row plus a notification -- the same
    // mechanism already built for a second-degree "connect" tap in the
    // feed, just with a different message. Deliberately not delegated to
    // ConnectionRequestService, which already depends on this class.
    private void sendApprovalRequest(AppUser owner, AppUser contact) {
        ConnectionRequest cr = connectionRequestRepository
                .findBySenderIdAndRecipientIdAndStatus(owner.getId(), contact.getId(), "PENDING")
                .orElseGet(ConnectionRequest::new);
        cr.setSenderId(owner.getId());
        cr.setRecipientId(contact.getId());
        cr.setStatus("PENDING");
        cr = connectionRequestRepository.save(cr);

        notificationService.notify(
                contact.getId(),
                "CONNECTION_REQUEST_RECEIVED",
                owner.getId(),
                owner.getDisplayName(),
                owner.getDisplayName() + " wants to add you to their circle.",
                cr.getId(),
                null);
    }

    private FriendConnection upsertActive(String ownerTieyohId, AppUser contact) {
        FriendConnection fc = friendConnectionRepository
                .findByOwnerTieyohIdAndContactUserId(ownerTieyohId, contact.getId())
                .orElseGet(FriendConnection::new);
        fc.setOwnerTieyohId(ownerTieyohId);
        fc.setContactUserId(contact.getId());
        fc.setDisplayName(contact.getDisplayName());
        fc.setContactTieyohId(contact.getTieyohId());
        fc.setPhone(contact.getPhone());
        fc.setEmail(contact.getEmail());
        fc.setStatus("ACTIVE");
        return friendConnectionRepository.save(fc);
    }

    // Called once a user finishes profile setup (real @t id + phone on
    // file) -- anyone who'd invited this phone or email as a pending
    // circle entry now gets resolved the same way a search-based add of an
    // existing account would: mutual and immediate if this user
    // auto-accepts, or a PENDING_APPROVAL placeholder plus a request to
    // approve if they don't.
    public void promotePendingConnections(AppUser newlyCompletedUser) {
        if (!"ACTIVE".equals(newlyCompletedUser.getStatus())) {
            return;
        }
        if (newlyCompletedUser.getPhone() != null) {
            for (FriendConnection fc : friendConnectionRepository.findByStatusAndPhone("PENDING", newlyCompletedUser.getPhone())) {
                if (!fc.getOwnerTieyohId().equals(newlyCompletedUser.getTieyohId())) {
                    promote(fc, newlyCompletedUser);
                }
            }
        }
        if (newlyCompletedUser.getEmail() != null) {
            for (FriendConnection fc : friendConnectionRepository.findByStatusAndEmailIgnoreCase("PENDING", newlyCompletedUser.getEmail())) {
                if (!fc.getOwnerTieyohId().equals(newlyCompletedUser.getTieyohId())) {
                    promote(fc, newlyCompletedUser);
                }
            }
        }
    }

    private void promote(FriendConnection fc, AppUser user) {
        fc.setContactUserId(user.getId());
        fc.setDisplayName(user.getDisplayName());
        fc.setContactTieyohId(user.getTieyohId());

        if (Boolean.TRUE.equals(user.getAutoAcceptCircleInvites())) {
            fc.setStatus("ACTIVE");
            friendConnectionRepository.save(fc);
            appUserRepository.findByTieyohIdIgnoreCase(fc.getOwnerTieyohId())
                    .ifPresent(inviter -> {
                        upsertActive(user.getTieyohId(), inviter);
                        notifyAddedToCircle(inviter, user);
                    });
            return;
        }

        fc.setStatus("PENDING_APPROVAL");
        friendConnectionRepository.save(fc);
        appUserRepository.findByTieyohIdIgnoreCase(fc.getOwnerTieyohId())
                .ifPresent(inviter -> sendApprovalRequest(inviter, user));
    }

    private FriendResult toResult(FriendConnection fc) {
        String profilePicture = null;
        if (fc.getContactUserId() != null) {
            profilePicture = appUserRepository.findById(fc.getContactUserId())
                    .map(AppUser::getProfilePictureBase64)
                    .orElse(null);
        }
        return new FriendResult(
                fc.getId(),
                fc.getContactUserId(),
                fc.getDisplayName(),
                fc.getContactTieyohId(),
                fc.getPhone(),
                fc.getEmail(),
                fc.getStatus(),
                profilePicture);
    }
}
