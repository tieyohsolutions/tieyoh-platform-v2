package com.tieyoh.api.auth.dto;

// Lightweight, privacy-conscious shape for the invite search bar: never
// exposes another user's full phone/email, only a masked version, since
// this endpoint has no auth layer gating who can query it (prototype-grade,
// same trust level as the rest of this backend).
public class UserSearchResult {

    private Long id;
    private String tieyohId;
    private String displayName;
    private String maskedPhone;
    private String maskedEmail;
    private String profilePictureBase64;

    public UserSearchResult() {
    }

    public UserSearchResult(Long id, String tieyohId, String displayName, String maskedPhone, String maskedEmail, String profilePictureBase64) {
        this.id = id;
        this.tieyohId = tieyohId;
        this.displayName = displayName;
        this.maskedPhone = maskedPhone;
        this.maskedEmail = maskedEmail;
        this.profilePictureBase64 = profilePictureBase64;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTieyohId() {
        return tieyohId;
    }

    public void setTieyohId(String tieyohId) {
        this.tieyohId = tieyohId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public String getProfilePictureBase64() {
        return profilePictureBase64;
    }

    public void setProfilePictureBase64(String profilePictureBase64) {
        this.profilePictureBase64 = profilePictureBase64;
    }
}
