package com.tieyoh.api.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Dev-mode stand-in for real email delivery. No SMTP/SendGrid/Azure
// Communication Services is configured yet, so this just logs the code
// where the Spring Boot console can see it — good enough to test the
// whole signup flow end to end without a paid provider. Replace this
// class with a real OtpMailSender implementation once credentials are
// available; nothing else in OtpService needs to change.
@Service
public class LoggingOtpMailSender implements OtpMailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpMailSender.class);

    @Override
    public void send(String toEmail, String code, String purpose) {
        log.info("[DEV EMAIL] To: {} | Purpose: {} | Tieyoh verification code: {}", toEmail, purpose, code);
    }
}
