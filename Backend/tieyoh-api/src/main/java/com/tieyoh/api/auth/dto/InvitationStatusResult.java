package com.tieyoh.api.auth.dto;

// Returned when someone opens https://tieyoh.com/i/{token} -- tells the
// caller (a future web landing page, or a deep-link handler in the app)
// whether the token is still good and who it's for, without ever
// exposing the inviter's or recipient's contact details.
public class InvitationStatusResult {

    private boolean valid;
    private String status;
    private String recipientName;
    private Long inviterUserId;

    public InvitationStatusResult() {
    }

    public InvitationStatusResult(boolean valid, String status, String recipientName, Long inviterUserId) {
        this.valid = valid;
        this.status = status;
        this.recipientName = recipientName;
        this.inviterUserId = inviterUserId;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public Long getInviterUserId() {
        return inviterUserId;
    }

    public void setInviterUserId(Long inviterUserId) {
        this.inviterUserId = inviterUserId;
    }
}
