package com.tieyoh.api.auth.service;

// Abstraction over however the OTP code actually gets to the user.
// OtpService only depends on this interface, so swapping the dev-mode
// LoggingOtpMailSender for a real SMTP/SendGrid/Azure Communication
// Services implementation later is a one-file change.
public interface OtpMailSender {

    void send(String toEmail, String code, String purpose);
}
