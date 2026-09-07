package com.tieyoh.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class OtpRequestRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String purpose;

    // Optional -- only sent (and only checked) for SIGNUP, so the
    // email/phone pair can be validated together before a code is even
    // generated. Absent for LOGIN-purpose OTP requests, if any exist.
    private String phone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
