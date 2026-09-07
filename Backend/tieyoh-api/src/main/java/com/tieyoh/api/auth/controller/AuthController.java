package com.tieyoh.api.auth.controller;

import com.tieyoh.api.auth.dto.AuthResponse;
import com.tieyoh.api.auth.dto.LoginRequest;
import com.tieyoh.api.auth.dto.OtpRequestRequest;
import com.tieyoh.api.auth.dto.OtpVerifyRequest;
import com.tieyoh.api.auth.dto.SignupRequest;
import com.tieyoh.api.auth.service.AuthService;
import com.tieyoh.api.auth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    private static final String SIGNUP_OTP_PURPOSE = "SIGNUP";

    @PostMapping("/otp/request")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody OtpRequestRequest request) {
        // For signup, fail fast on a duplicate email or phone instead of
        // sending a code the account creation step will reject anyway --
        // the two are treated as a tightly-bound pair, neither can already
        // belong to an account.
        if (SIGNUP_OTP_PURPOSE.equals(request.getPurpose())) {
            authService.assertEmailAvailableForSignup(request.getEmail());
            authService.assertPhoneAvailableForSignup(request.getPhone());
        }
        otpService.requestCode(request.getEmail(), request.getPurpose());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Void> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        otpService.verifyCode(request.getEmail(), request.getPurpose(), request.getCode());
        return ResponseEntity.noContent().build();
    }
}
