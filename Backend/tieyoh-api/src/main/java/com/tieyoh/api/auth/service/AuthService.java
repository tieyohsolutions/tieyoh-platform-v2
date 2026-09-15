package com.tieyoh.api.auth.service;

import com.tieyoh.api.auth.dto.AuthResponse;
import com.tieyoh.api.auth.dto.LoginRequest;
import com.tieyoh.api.auth.dto.SignupRequest;
import com.tieyoh.api.auth.entity.AppUser;
import com.tieyoh.api.auth.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String SIGNUP_OTP_PURPOSE = "SIGNUP";

    private final AppUserRepository appUserRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository appUserRepository, OtpService otpService) {
        this.appUserRepository = appUserRepository;
        this.otpService = otpService;
    }

    // Called before an OTP is even sent, so a duplicate email is caught on
    // the first screen of signup instead of only surfacing after the user
    // has already gone through the verification-code step.
    public void assertEmailAvailableForSignup(String email) {
        String normalized = email.trim().toLowerCase();
        if (appUserRepository.existsByEmailIgnoreCase(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
    }

    // Email and phone are treated as a tightly-bound pair at signup --
    // neither can already belong to an account, since either one will
    // later be usable to look the account up (e.g. password reset).
    // Mirrors assertEmailAvailableForSignup above; a blank/unparseable
    // phone normalizes to null and is treated as "nothing to check" here
    // rather than rejected -- OtpRequestRequest.phone is optional.
    public void assertPhoneAvailableForSignup(String phone) {
        String normalized = com.tieyoh.api.auth.service.ContactNormalizer.normalizePhone(phone);
        if (normalized != null && appUserRepository.existsByPhone(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }
    }

    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        if (!otpService.isRecentlyVerified(email, SIGNUP_OTP_PURPOSE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please verify your email with the code we sent before creating an account.");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName().trim());
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            // Re-check here too (defense in depth against the otp/request
            // check being raced by two signups) rather than trusting that
            // an earlier screen already confirmed it.
            assertPhoneAvailableForSignup(request.getPhone());
            user.setPhone(com.tieyoh.api.auth.service.ContactNormalizer.normalizePhone(request.getPhone()));
        }
        // Profile setup (Tieyoh ID / gender / birthday / etc.) is a
        // separate set of screens right after this, not part of signup
        // itself -- PENDING_PROFILE marks the account as not fully
        // onboarded yet, so it stays invisible to search/invites/circles
        // until that screen completes (see UserProfileService.saveProfile,
        // which flips this to ACTIVE).
        user.setStatus("PENDING_PROFILE");

        AppUser saved = appUserRepository.save(user);

        return toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        // PENDING_PROFILE accounts can still log back in -- they just
        // haven't finished the post-signup profile setup screen yet (e.g.
        // they backed out or the app was killed). Any other non-ACTIVE
        // status (disabled, banned, etc.) stays blocked.
        if (!"ACTIVE".equals(user.getStatus()) && !"PENDING_PROFILE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not active");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return toResponse(user);
    }

    private AuthResponse toResponse(AppUser user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getTieyohId(), user.getProfilePictureBase64(), user.getAutoAcceptCircleInvites());
    }
}
