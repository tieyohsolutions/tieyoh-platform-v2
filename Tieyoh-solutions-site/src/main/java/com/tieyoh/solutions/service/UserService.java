package com.tieyoh.solutions.service;

import com.tieyoh.solutions.domain.Role;
import com.tieyoh.solutions.domain.User;
import com.tieyoh.solutions.domain.UserStatus;
import com.tieyoh.solutions.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public User registerSelfSignup(String name, String email, String company, String title, String rawPassword) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = User.builder()
                .name(name)
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .company(company)
                .title(title)
                .role(Role.MEMBER)
                .status(UserStatus.PENDING_APPROVAL)
                .build();
        User saved = userRepository.save(user);
        notificationService.sendNewSignupAlert(saved.getName(), saved.getEmail(), saved.getCompany());
        return saved;
    }

    @Transactional
    public String inviteMember(String name, String email, String company, String title) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        String tempPassword = generateTempPassword();
        User user = User.builder()
                .name(name)
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .company(company)
                .title(title)
                .role(Role.MEMBER)
                .status(UserStatus.ACTIVE)
                .approvedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        return tempPassword;
    }

    @Transactional
    public void approve(Long userId) {
        User user = getById(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setApprovedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void reject(Long userId) {
        User user = getById(userId);
        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
    }

    @Transactional
    public void disable(Long userId) {
        User user = getById(userId);
        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
    }

    @Transactional
    public void setRole(Long userId, Role role) {
        User user = getById(userId);
        user.setRole(role);
        userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public List<User> pendingApprovals() {
        return userRepository.findByStatusOrderByCreatedAtDesc(UserStatus.PENDING_APPROVAL);
    }

    public List<User> allUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder("Tieyoh-");
        for (int i = 0; i < 10; i++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
