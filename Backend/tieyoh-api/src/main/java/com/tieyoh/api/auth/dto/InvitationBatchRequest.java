package com.tieyoh.api.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

// NOTE ON inviterTieyohId: the spec this DTO was built from calls for the
// inviter to be "determined from the authenticated account," never trusted
// from the client. This backend has no session/bearer-token auth yet (see
// tieyoh-auth-architecture.md -- phases 1-2 shipped are OTP + device-local
// biometric unlock only, no backend-verified session token), and every
// other endpoint in this app already takes a plain client-supplied
// identifier the same way (friends, notifications, connection requests,
// etc). This DTO follows that same existing trust model for consistency
// rather than silently doing something different here alone. Once real
// session auth exists, this field should be removed in favor of deriving
// the inviter from the authenticated request.
public class InvitationBatchRequest {

    @NotBlank(message = "inviterTieyohId is required")
    private String inviterTieyohId;
    private List<@Valid InvitationRecipientRequest> recipients;

    public InvitationBatchRequest() {
    }

    public String getInviterTieyohId() {
        return inviterTieyohId;
    }

    public void setInviterTieyohId(String inviterTieyohId) {
        this.inviterTieyohId = inviterTieyohId;
    }

    public List<InvitationRecipientRequest> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<InvitationRecipientRequest> recipients) {
        this.recipients = recipients;
    }
}
