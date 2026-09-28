package com.tieyoh.api.auth.service;

import com.tieyoh.api.auth.dto.AuthResponse;
import com.tieyoh.api.auth.dto.ProfileSetupRequest;
import com.tieyoh.api.auth.dto.UserSearchResult;
import com.tieyoh.api.auth.dto.UserProfileResponse;
import com.tieyoh.api.auth.dto.UserSettingsRequest;
import com.tieyoh.api.auth.entity.AppUser;
import com.tieyoh.api.auth.entity.UserMerchantList;
import com.tieyoh.api.auth.repository.AppUserRepository;
import com.tieyoh.api.auth.repository.UserMerchantListRepository;
import com.tieyoh.api.auth.service.FriendService;
import com.tieyoh.api.gift.repository.GiftRepository;
import com.tieyoh.api.merchant.dto.MerchantMobileResponse;
import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.repository.MerchantRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    private final AppUserRepository appUserRepository;
    private final UserMerchantListRepository userMerchantListRepository;
    private final MerchantRepository merchantRepository;
    private final FriendService friendService;
    private final GiftRepository giftRepository;

    public UserProfileService(
            AppUserRepository appUserRepository,
            UserMerchantListRepository userMerchantListRepository,
            MerchantRepository merchantRepository,
            FriendService friendService,
            GiftRepository giftRepository) {
        this.appUserRepository = appUserRepository;
        this.userMerchantListRepository = userMerchantListRepository;
        this.merchantRepository = merchantRepository;
        this.friendService = friendService;
        this.giftRepository = giftRepository;
    }

    public boolean isTieyohIdAvailable(String rawId) {
        String id = normalizeId(rawId);
        return !appUserRepository.existsByTieyohIdIgnoreCase(id);
    }

    public AuthResponse saveProfile(Long userId, ProfileSetupRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String tieyohId = normalizeId(request.getTieyohId());

        appUserRepository.findByTieyohIdIgnoreCase(tieyohId)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "That Tieyoh ID is already taken");
                });

        // Optional -- Edit Profile is the only caller that ever sends this
        // (signup sets email through AuthService, not here). Normalized the
        // same way AuthService does (trim + lowercase) so it matches
        // whatever case the uniqueness check and future logins use.
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
                appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                        .filter(existing -> !existing.getId().equals(userId))
                        .ifPresent(existing -> {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email address is already in use");
                        });
                user.setEmail(normalizedEmail);
            }
        }

        user.setTieyohId(tieyohId);

        // Phone is optional on this screen now -- the new onboarding flow
        // already persisted it at signup (AuthService.signup). Only touch
        // it here if a caller (e.g. a future Edit Profile update) actually
        // sends one; leaving it null/blank must NOT wipe out what's
        // already on the account. Same tightly-bound-with-email treatment
        // as tieyohId/email above when a value is supplied: reject if it's
        // already someone else's. Normalized the same way
        // FriendService/InvitationService normalize an invited contact's
        // phone, for consistent matching everywhere.
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String normalizedPhone = ContactNormalizer.normalizePhone(request.getPhone());
            if (normalizedPhone != null) {
                appUserRepository.findByPhone(normalizedPhone)
                        .filter(existing -> !existing.getId().equals(userId))
                        .ifPresent(existing -> {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "That phone number is already in use");
                        });
                user.setPhone(normalizedPhone);
            }
        }
        user.setAddress(request.getAddress() == null || request.getAddress().isBlank()
                ? null
                : request.getAddress().trim());
        user.setGender(request.getGender());
        // Optional -- see ProfileSetupRequest for why this isn't required.
        // Only touched when supplied, so re-saving the profile later (this
        // same endpoint) never wipes out a birthday that was set before.
        if (request.getBirthMonth() != null) {
            user.setBirthMonth(request.getBirthMonth());
        }
        if (request.getBirthDay() != null) {
            user.setBirthDay(request.getBirthDay());
        }
        if (request.getHolidaysCelebrated() != null) {
            user.setHolidaysCelebrated(request.getHolidaysCelebrated());
        }
        // Completing this screen is what makes the account a real,
        // discoverable Tieyoh user -- flip PENDING_PROFILE (set at signup)
        // to ACTIVE now that there's a @t id and phone on file. Leaves any
        // other status (e.g. a future disabled/banned state) untouched.
        if ("PENDING_PROFILE".equals(user.getStatus())) {
            user.setStatus("ACTIVE");
        }

        AppUser saved = appUserRepository.save(user);

        // Now that this user has a real @t id and phone on file, anyone
        // who'd already invited them by that phone/email gets promoted
        // from a pending, greyed-out invite to a full, active circle entry.
        friendService.promotePendingConnections(saved);

        return new AuthResponse(saved.getId(), saved.getEmail(), saved.getDisplayName(), saved.getTieyohId(), saved.getProfilePictureBase64(), saved.getAutoAcceptCircleInvites());
    }

    // Backs the Edit Profile screen: lets it pre-fill birthday, holidays,
    // and the fields it must resend unchanged (tieyohId/phone/address/
    // gender are all "required" on ProfileSetupRequest, not partial-update
    // fields) without relying on the AuthResponse the app already holds,
    // which never carried this level of detail.
    public UserProfileResponse getProfile(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new UserProfileResponse(
                user.getEmail(),
                user.getTieyohId(),
                user.getPhone(),
                user.getAddress(),
                user.getGender(),
                user.getBirthMonth(),
                user.getBirthDay(),
                user.getHolidaysCelebrated(),
                user.getAutoAcceptCircleInvites(),
                user.getShareGiftingWithCircle());
    }

    // Backs Edit Profile's Privacy section: "auto-accept circle invites"
    // and "share gifting with your circle" toggles. Only non-null fields
    // are applied, so this DTO can grow to cover more settings later
    // without breaking older clients that only ever send the ones they
    // know about.
    public AuthResponse updateSettings(Long userId, UserSettingsRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getAutoAcceptCircleInvites() != null) {
            user.setAutoAcceptCircleInvites(request.getAutoAcceptCircleInvites());
        }
        if (request.getShareGiftingWithCircle() != null) {
            user.setShareGiftingWithCircle(request.getShareGiftingWithCircle());
        }

        AppUser saved = appUserRepository.save(user);
        return new AuthResponse(saved.getId(), saved.getEmail(), saved.getDisplayName(), saved.getTieyohId(),
                saved.getProfilePictureBase64(), saved.getAutoAcceptCircleInvites());
    }

    // Full replace -- used by onboarding's "What places make you happy?"
    // step, which always submits the complete set it wants. NOT used by
    // "Add a merchant" from My picks, since that must be additive and
    // must not disturb targets already set on existing picks -- see
    // addMerchantToList below. Open-ended: no cap on list size.
    public void saveMerchantSelections(Long userId, List<Long> merchantIds) {
        List<Long> ids = merchantIds == null ? List.of() : merchantIds;

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        userMerchantListRepository.deleteByUserId(userId);

        for (Long merchantId : ids) {
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found: " + merchantId));

            UserMerchantList entry = new UserMerchantList();
            entry.setUser(user);
            entry.setMerchant(merchant);
            userMerchantListRepository.save(entry);
        }
    }

    // Backs "Add a merchant" from My picks -- additive, unlike
    // saveMerchantSelections above. Adding a merchant that's already on
    // the list is a no-op on membership (no duplicate row) but still
    // lets a target be attached if one wasn't set before. A user can
    // pick a merchant with no target here, or set one in the same step;
    // either way it never touches any other pick already on the list.
    public void addMerchantToList(Long userId, Long merchantId, BigDecimal targetAmount) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found: " + merchantId));

        UserMerchantList entry = userMerchantListRepository
                .findByUserIdAndMerchantId(userId, merchantId)
                .orElseGet(() -> {
                    UserMerchantList created = new UserMerchantList();
                    created.setUser(user);
                    created.setMerchant(merchant);
                    return created;
                });

        if (targetAmount != null) {
            entry.setTargetAmount(targetAmount);
        }

        userMerchantListRepository.save(entry);
    }

    // Backs the "Set target" / "Edit target" action on an existing pick.
    // 404s if the merchant isn't actually on this user's list rather than
    // silently creating one -- that path is addMerchantToList's job.
    // Passing a null targetAmount clears a previously-set target.
    public void updateMerchantTarget(Long userId, Long merchantId, BigDecimal targetAmount) {
        UserMerchantList entry = userMerchantListRepository
                .findByUserIdAndMerchantId(userId, merchantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "That merchant isn't on this user's list"));

        entry.setTargetAmount(targetAmount);
        userMerchantListRepository.save(entry);
    }

    // Backs the Send Gift flow's "their favorite places" step AND "My
    // picks" -- same shape for both, with targetAmount/amountReceived
    // simply unused by the Send Gift screen. Empty list (not an error)
    // if they haven't picked any yet. amountReceived is derived live from
    // completed Gift rows, not a stored running total.
    public List<MerchantMobileResponse> getMerchantSelections(Long userId) {
        List<UserMerchantList> entries = userMerchantListRepository.findByUserId(userId);

        Map<Long, BigDecimal> receivedByMerchant = giftRepository
                .sumReceivedByRecipientGroupedByMerchant(userId)
                .stream()
                .collect(Collectors.toMap(
                        GiftRepository.MerchantReceivedTotal::getMerchantId,
                        GiftRepository.MerchantReceivedTotal::getTotal
                ));

        return entries.stream()
                .map(entry -> toMobileResponse(entry, receivedByMerchant.get(entry.getMerchant().getId())))
                .collect(Collectors.toList());
    }

    private MerchantMobileResponse toMobileResponse(UserMerchantList entry, BigDecimal amountReceived) {
        Merchant merchant = entry.getMerchant();
        MerchantMobileResponse response = new MerchantMobileResponse();
        response.setId(merchant.getId());
        response.setTmid(merchant.getTmid());
        response.setBrandName(merchant.getBrandName());
        response.setLogoUrl(merchant.getLogoUrl());
        response.setCategory(merchant.getCategory());
        response.setStatus(merchant.getStatus());
        // Missing here was the whole reason a partnered merchant's logo
        // never showed on "My Gift Cards"/"My picks" -- the mobile app's
        // MerchantBalanceRow/MerchantPickCard only render a logo when
        // partnered is true, and this response object was leaving that
        // field at its default (null), regardless of the real value in
        // the merchant table.
        response.setPartnered(merchant.isPartnered());
        if (merchant.getParentMerchant() != null) {
            response.setPmid(merchant.getParentMerchant().getPmid());
        }
        response.setTargetAmount(entry.getTargetAmount());
        response.setAmountReceived(amountReceived == null ? BigDecimal.ZERO : amountReceived);
        return response;
    }

    // Backs the invite screen's search bar: a partial match on @t handle,
    // phone, or email, returned with contact info masked since there's no
    // auth layer here to gate who can look someone else up. Blank queries
    // short-circuit to no results rather than dumping the whole user table.
    public List<UserSearchResult> searchUsers(String query, Long excludeUserId) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return Collections.emptyList();
        }

        // "@t-" is the fixed, default handle label -- the user shouldn't
        // have to type it. Strip a leading "@t-" or "t-" if they typed it
        // out of habit anyway, so "test", "t-test", and "@t-test" all
        // search identically against the handle body.
        String handleQuery = q.replaceFirst("(?i)^@?t-", "");

        List<AppUser> matches = appUserRepository.searchByHandlePhoneOrEmail(
                handleQuery, q, excludeUserId, PageRequest.of(0, 20));

        return matches.stream()
                .map(u -> new UserSearchResult(
                        u.getId(),
                        u.getTieyohId(),
                        u.getDisplayName(),
                        maskPhone(u.getPhone()),
                        maskEmail(u.getEmail()),
                        u.getProfilePictureBase64()))
                .collect(Collectors.toList());
    }

    // Called after the mobile app resizes/compresses a newly-picked photo
    // and base64-encodes it -- keeps the whole thing a plain JSON PATCH
    // rather than standing up multipart upload handling for a prototype.
    public AuthResponse updateProfilePicture(Long userId, String imageBase64) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setProfilePictureBase64(imageBase64 == null || imageBase64.isBlank() ? null : imageBase64);
        AppUser saved = appUserRepository.save(user);

        return new AuthResponse(saved.getId(), saved.getEmail(), saved.getDisplayName(), saved.getTieyohId(), saved.getProfilePictureBase64(), saved.getAutoAcceptCircleInvites());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return "***";
        }
        return "***-***-" + digits.substring(digits.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String name = email.substring(0, at);
        String domain = email.substring(at);
        String maskedName = name.length() <= 1 ? "*" : (name.charAt(0) + "***");
        return maskedName + domain;
    }

    // "@t-" is a fixed label the client always sends as part of the full
    // ID now, but this stays defensive in case a caller sends just the
    // handle, or a legacy bare "@t" from before this format existed.
    private String normalizeId(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("@")) {
            trimmed = "@" + trimmed;
        }
        if (!trimmed.startsWith("@t-")) {
            String suffix = trimmed.startsWith("@t") ? trimmed.substring(2) : trimmed.substring(1);
            if (suffix.startsWith("-")) {
                suffix = suffix.substring(1);
            }
            trimmed = "@t-" + suffix;
        }
        return trimmed;
    }
}
