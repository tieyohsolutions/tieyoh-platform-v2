package com.tieyoh.api.auth.controller;

import com.tieyoh.api.auth.dto.AuthResponse;
import com.tieyoh.api.auth.dto.MerchantSelectionRequest;
import com.tieyoh.api.auth.dto.MerchantTargetRequest;
import com.tieyoh.api.auth.dto.ProfilePictureRequest;
import com.tieyoh.api.auth.dto.ProfileSetupRequest;
import com.tieyoh.api.auth.dto.UserProfileResponse;
import com.tieyoh.api.auth.dto.UserSearchResult;
import com.tieyoh.api.auth.dto.UserSettingsRequest;
import com.tieyoh.api.auth.service.UserProfileService;
import com.tieyoh.api.merchant.dto.MerchantMobileResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/check-id")
    public Map<String, Boolean> checkIdAvailable(@RequestParam String id) {
        return Map.of("available", userProfileService.isTieyohIdAvailable(id));
    }

    @PatchMapping("/{id}/profile")
    public AuthResponse saveProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfileSetupRequest request) {
        return userProfileService.saveProfile(id, request);
    }

    // Backs the Edit Profile screen -- lets it pre-fill birthday/holidays
    // (and the fields it must resend unchanged) without relying on the
    // AuthResponse already held client-side.
    @GetMapping("/{id}/profile")
    public UserProfileResponse getProfile(@PathVariable Long id) {
        return userProfileService.getProfile(id);
    }

    @PostMapping("/{id}/merchants")
    public void saveMerchantSelections(
            @PathVariable Long id,
            @RequestBody MerchantSelectionRequest request) {
        userProfileService.saveMerchantSelections(id, request.getMerchantIds());
    }

    // Backs the Send Gift flow's "Their favorite places" step.
    @GetMapping("/{id}/merchants")
    public List<MerchantMobileResponse> getMerchantSelections(@PathVariable Long id) {
        return userProfileService.getMerchantSelections(id);
    }

    // Backs "Add a merchant to your list" on My picks -- additive, unlike
    // the full-replace POST /{id}/merchants above. Body is optional; a
    // merchant can be added with no target and one set later via the
    // /target endpoint below.
    @PostMapping("/{id}/merchants/{merchantId}")
    public void addMerchantToList(
            @PathVariable Long id,
            @PathVariable Long merchantId,
            @RequestBody(required = false) MerchantTargetRequest request) {
        userProfileService.addMerchantToList(
                id,
                merchantId,
                request == null ? null : request.getTargetAmount());
    }

    // Backs "Set target" / "Edit target" on an existing My picks entry.
    @PatchMapping("/{id}/merchants/{merchantId}/target")
    public void updateMerchantTarget(
            @PathVariable Long id,
            @PathVariable Long merchantId,
            @RequestBody MerchantTargetRequest request) {
        userProfileService.updateMerchantTarget(id, merchantId, request.getTargetAmount());
    }

    @PatchMapping("/{id}/profile-picture")
    public AuthResponse updateProfilePicture(
            @PathVariable Long id,
            @RequestBody ProfilePictureRequest request) {
        return userProfileService.updateProfilePicture(id, request.getImageBase64());
    }

    // Backs the Profile screen's settings toggles (currently just
    // "auto-accept circle invites").
    @PatchMapping("/{id}/settings")
    public AuthResponse updateSettings(
            @PathVariable Long id,
            @RequestBody UserSettingsRequest request) {
        return userProfileService.updateSettings(id, request);
    }

    // Backs the invite screen's search bar: type an @t id, phone number,
    // or email fragment and get back matching Tieyoh users (contact info
    // masked). excludeUserId keeps the searching user from matching
    // themselves -- there's no auth context to infer that server-side.
    @GetMapping("/search")
    public List<UserSearchResult> search(
            @RequestParam("q") String q,
            @RequestParam("excludeUserId") Long excludeUserId) {
        return userProfileService.searchUsers(q, excludeUserId);
    }
}
