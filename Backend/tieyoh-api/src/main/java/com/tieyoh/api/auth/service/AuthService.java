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

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName().trim());

        AppUser saved = appUserRepository.save(user);

        return toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not active");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return toResponse(user);
    }

    private AuthResponse toResponse(AppUser user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
