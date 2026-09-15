package com.tieyoh.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String displayName;

    // Optional -- the onboarding redesign's "Let's get started" screen
    // collects these as two separate boxes above the phone number field.
    // Not required here (yet) because the existing in-app "Sign up" toggle
    // on LoginScreen still submits displayName alone; once the redesigned
    // flow's later screens (password, verify code) are built and this
    // becomes the only way to sign up, these can become required and
    // displayName can be derived from them instead.
    private String firstName;

    private String lastName;

    // Optional -- the new onboarding flow collects and validates this on
    // "Let's get started" (checked for availability there too), then
    // carries it through to here so the account is created with it
    // already attached instead of asking again later. Absent for the old
    // in-app LoginScreen signup toggle, which never collects a phone.
    private String phone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
