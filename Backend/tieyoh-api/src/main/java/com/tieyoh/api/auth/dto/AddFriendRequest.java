package com.tieyoh.api.auth.dto;

import jakarta.validation.constraints.Email;

// Body for POST /api/users/{ownerTieyohId}/friends. Two shapes in one DTO:
// -- contactUserId set: adding someone found via the Tieyoh user search
//    (already has an account) -- displayName/phone/email are ignored,
//    the server fills them in from that account.
// -- contactUserId null: a raw invite by phone or email for someone not
//    (yet) on Tieyoh -- displayName plus one of phone/email is required.
public class AddFriendRequest {

    private Long contactUserId;
    private String displayName;
    private String phone;

    @Email(message = "That doesn't look like a valid email address")
    private String email;

    public AddFriendRequest() {
    }

    public Long getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(Long contactUserId) {
        this.contactUserId = contactUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
