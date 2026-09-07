package com.tieyoh.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileSetupRequest {

    @NotBlank(message = "Please choose a Tieyoh ID")
    @Pattern(
            regexp = "^@t-[A-Za-z0-9_]{2,15}$",
            message = "ID must be in the form @t-<handle> (2-15 letters, numbers, or underscores after the dash)"
    )
    private String tieyohId;

    // Optional -- omitted or blank means "leave the current email alone."
    // Added for the Edit Profile screen; signup itself sets email through
    // AuthService, not this endpoint. UserProfileService checks this isn't
    // already someone else's email before applying it (the column has its
    // own DB-level unique constraint too, as a last resort).
    @Email(message = "That doesn't look like a valid email address")
    private String email;

    // No longer required on this screen -- the new onboarding flow
    // captures and persists phone at signup (see AuthService.signup).
    // Still accepted so Edit Profile (or any future path) can update it.
    private String phone;

    private String address;

    @NotBlank(message = "Please select Male, Female, or Other")
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Sex must be Male, Female, or Other")
    private String gender;

    // Both optional -- like address, birthday isn't gated on to keep this
    // screen from blocking signup (the original spec called address
    // "Mandatory" too, but it was implemented as optional; matching that
    // same precedent here rather than introducing a second, stricter
    // required-field policy just for this one). Bean Validation's
    // @Min/@Max pass a null value through untouched -- they only fire
    // once a value is actually supplied.
    @Min(value = 1, message = "Birth month must be between 1 and 12")
    @Max(value = 12, message = "Birth month must be between 1 and 12")
    private Integer birthMonth;

    @Min(value = 1, message = "Birth day must be between 1 and 31")
    @Max(value = 31, message = "Birth day must be between 1 and 31")
    private Integer birthDay;

    // Comma-separated labels from a fixed mobile-side picklist -- see
    // AppUser.holidaysCelebrated. Optional, same as birthday.
    @Size(max = 500, message = "That's too many holidays selected")
    private String holidaysCelebrated;

    public String getTieyohId() {
        return tieyohId;
    }

    public void setTieyohId(String tieyohId) {
        this.tieyohId = tieyohId;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getBirthMonth() {
        return birthMonth;
    }

    public void setBirthMonth(Integer birthMonth) {
        this.birthMonth = birthMonth;
    }

    public Integer getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Integer birthDay) {
        this.birthDay = birthDay;
    }

    public String getHolidaysCelebrated() {
        return holidaysCelebrated;
    }

    public void setHolidaysCelebrated(String holidaysCelebrated) {
        this.holidaysCelebrated = holidaysCelebrated;
    }
}
