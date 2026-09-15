package com.tieyoh.api.auth.controller;

import com.tieyoh.api.auth.dto.InvitationBatchRequest;
import com.tieyoh.api.auth.dto.InvitationBatchResponse;
import com.tieyoh.api.auth.dto.InvitationStatusResult;
import com.tieyoh.api.auth.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// POST /api/invitations/batch -- called from the multi-contact picker
// (login screen or the + button) once the user has selected specific
// contacts; the client must only ever send the contacts the user picked,
// never a whole address book. Each recipient in the request gets back its
// own separately-redeemable invitation link -- the client hands those to
// the OS share sheet, it never sends anything itself.
//
// GET /api/invitations/{token} -- lets a future landing page or deep-link
// handler check whether a link is still good before showing a signup
// screen, without exposing either side's contact details.
//
// POST /api/invitations/{token}/accept -- called by the mobile app once a
// brand-new account (created after opening this exact invite link)
// finishes profile setup, so the backend can link the inviter and the new
// account directly rather than relying on phone/email matching.
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/batch")
    public InvitationBatchResponse createBatch(@Valid @RequestBody InvitationBatchRequest request) {
        return invitationService.createBatch(request.getInviterTieyohId(), request.getRecipients());
    }

    @GetMapping("/{token}")
    public InvitationStatusResult getStatus(@PathVariable String token) {
        return invitationService.resolveToken(token);
    }

    @PostMapping("/{token}/accept")
    public void accept(@PathVariable String token, @RequestParam Long userId) {
        invitationService.markAccepted(token, userId);
    }
}
