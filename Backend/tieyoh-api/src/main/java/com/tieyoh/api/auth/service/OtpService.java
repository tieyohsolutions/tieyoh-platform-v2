package com.tieyoh.api.auth.service;

import com.tieyoh.api.auth.entity.OtpCode;
import com.tieyoh.api.auth.repository.OtpCodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int VERIFIED_WINDOW_MINUTES = 15;

    private final OtpCodeRepository otpCodeRepository;
    private final OtpMailSender mailSender;
    private final PasswordEncoder codeEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeRepository otpCodeRepository, OtpMailSender mailSender) {
        this.otpCodeRepository = otpCodeRepository;
        this.mailSender = mailSender;
    }

    public void requestCode(String email, String purpose) {
        String contact = email.trim().toLowerCase();
        String code = generateCode();

        OtpCode otp = new OtpCode();
        otp.setContact(contact);
        otp.setPurpose(purpose);
        otp.setCodeHash(codeEncoder.encode(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        otpCodeRepository.save(otp);

        mailSender.send(contact, code, purpose);
    }

    public void verifyCode(String email, String purpose, String code) {
        String contact = email.trim().toLowerCase();

        OtpCode otp = otpCodeRepository
                .findTopByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(contact, purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No verification code found for this email — request a new one."));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That code expired — request a new one.");
        }

        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts — request a new code.");
        }

        if (!codeEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpCodeRepository.save(otp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That code isn't right — try again.");
        }

        otp.setConsumedAt(LocalDateTime.now());
        otpCodeRepository.save(otp);
    }

    // Used by AuthService.signup() as proof the email was verified recently,
    // instead of introducing a separate short-lived token type for now.
    public boolean isRecentlyVerified(String email, String purpose) {
        String contact = email.trim().toLowerCase();
        return otpCodeRepository.existsByContactAndPurposeAndConsumedAtIsNotNullAndConsumedAtAfter(
                contact, purpose, LocalDateTime.now().minusMinutes(VERIFIED_WINDOW_MINUTES));
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int value = random.nextInt(bound);
        return String.format("%0" + CODE_LENGTH + "d", value);
    }
}
