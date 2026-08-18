package com.tieyoh.solutions.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends internal notification emails (e.g. to the site admin) when things happen on the site,
 * such as a new signup request. Failures here are logged and swallowed rather than thrown, so a
 * misconfigured or unreachable mail server never blocks the actual user-facing action (like signup).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.notify.email:}")
    private String notifyEmail;

    @Value("${app.notify.sms-email:}")
    private String notifySmsEmail;

    public void sendNewSignupAlert(String name, String email, String company) {
        String companyLine = (company == null || company.isBlank()) ? "-" : company;
        String subject = "New Tieyoh Solutions signup request";
        String body = "Name: " + name
                + "\nEmail: " + email
                + "\nCompany: " + companyLine
                + "\n\nReview and approve or reject at https://tieyohsolutions.com/admin/members";
        sendIfConfigured(notifyEmail, subject, body);

        // Short version for the SMS-via-email-gateway copy -- carrier gateways often truncate
        // long messages, so keep this one brief.
        if (notifySmsEmail != null && !notifySmsEmail.isBlank()) {
            String smsBody = "Tieyoh signup request: " + name + " (" + email + ")";
            sendIfConfigured(notifySmsEmail, "Tieyoh signup", smsBody);
        }
    }

    private void sendIfConfigured(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send notification to {}: {}", to, ex.getMessage());
        }
    }
}
