package com.tieyoh.api.auth.dto;

import jakarta.validation.constraints.Email;

public class InvitationRecipientRequest {

    private String displayName;

    @Email(message = "That doesn't look like a valid email address")
    private String email;

    private String phone;

    public InvitationRecipientRequest() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
