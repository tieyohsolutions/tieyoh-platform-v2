package com.tieyoh.api.auth.dto;

public class AuthResponse {

    private Long id;
    private String email;
    private String displayName;
    private String tieyohId;
    private String profilePictureBase64;
    private Boolean autoAcceptCircleInvites;

    public AuthResponse() {
    }

    public AuthResponse(Long id, String email, String displayName, String tieyohId) {
        this(id, email, displayName, tieyohId, null, null);
    }

    public AuthResponse(Long id, String email, String displayName, String tieyohId, String profilePictureBase64) {
        this(id, email, displayName, tieyohId, profilePictureBase64, null);
    }

    public AuthResponse(Long id, String email, String displayName, String tieyohId,
                         String profilePictureBase64, Boolean autoAcceptCircleInvites) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.tieyohId = tieyohId;
        this.profilePictureBase64 = profilePictureBase64;
        this.autoAcceptCircleInvites = autoAcceptCircleInvites;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTieyohId() {
        return tieyohId;
    }

    public void setTieyohId(String tieyohId) {
        this.tieyohId = tieyohId;
    }

    public String getProfilePictureBase64() {
        return profilePictureBase64;
    }

    public void setProfilePictureBase64(String profilePictureBase64) {
        this.profilePictureBase64 = profilePictureBase64;
    }

    public Boolean getAutoAcceptCircleInvites() {
        return autoAcceptCircleInvites;
    }

    public void setAutoAcceptCircleInvites(Boolean autoAcceptCircleInvites) {
        this.autoAcceptCircleInvites = autoAcceptCircleInvites;
    }
}
