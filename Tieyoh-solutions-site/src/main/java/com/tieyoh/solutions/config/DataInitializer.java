package com.tieyoh.solutions.config;

import com.tieyoh.solutions.domain.Role;
import com.tieyoh.solutions.domain.User;
import com.tieyoh.solutions.domain.UserStatus;
import com.tieyoh.solutions.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        User admin = User.builder()
                .name(adminName)
                .email(adminEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .approvedAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);
        log.warn("Created default admin account ({}). Change the password immediately after first login.", adminEmail);
    }
}
