package com.tieyoh.api.auth.dto;

// Body for PATCH /api/users/{id}/profile-picture -- imageBase64 is a
// base64-encoded JPEG the mobile client has already resized/compressed.
public class ProfilePictureRequest {

    private String imageBase64;

    public ProfilePictureRequest() {
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
