package com.tieyoh.api.auth.service;

import com.tieyoh.api.auth.dto.ConnectionRequestResult;
import com.tieyoh.api.auth.entity.AppUser;
import com.tieyoh.api.auth.entity.ConnectionRequest;
import com.tieyoh.api.auth.repository.AppUserRepository;
import com.tieyoh.api.auth.repository.ConnectionRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Sent when a user taps a second-degree person in the feed's network
// section. Accepting one links both accounts via FriendService.connectMutually
// and notifies the original sender; declining just closes it out with no
// side effects.
@Service
public class ConnectionRequestService {

    private final ConnectionRequestRepository connectionRequestRepository;
    private final AppUserRepository appUserRepository;
    private final FriendService friendService;
    private final NotificationService notificationService;

    public ConnectionRequestService(ConnectionRequestRepository connectionRequestRepository,
                                     AppUserRepository appUserRepository,
                                     FriendService friendService,
                                     NotificationService notificationService) {
        this.connectionRequestRepository = connectionRequestRepository;
        this.appUserRepository = appUserRepository;
        this.friendService = friendService;
        this.notificationService = notificationService;
    }

    public ConnectionRequestResult sendRequest(Long senderId, Long recipientId) {
        if (senderId.equals(recipientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't send a connection request to yourself");
        }

        ConnectionRequest cr = connectionRequestRepository
                .findBySenderIdAndRecipientIdAndStatus(senderId, recipientId, "PENDING")
                .orElseGet(ConnectionRequest::new);
        cr.setSenderId(senderId);
        cr.setRecipientId(recipientId);
        cr.setStatus("PENDING");
        cr = connectionRequestRepository.save(cr);

        AppUser sender = appUserRepository.findById(senderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        notificationService.notify(
                recipientId,
                "CONNECTION_REQUEST_RECEIVED",
                senderId,
                sender.getDisplayName(),
                sender.getDisplayName() + " wants to connect with you.",
                cr.getId(),
                null);

        return toResult(cr);
    }

    public ConnectionRequestResult respond(Long requestId, boolean accept) {
        ConnectionRequest cr = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection request not found"));

        cr.setStatus(accept ? "ACCEPTED" : "DECLINED");
        cr = connectionRequestRepository.save(cr);

        if (accept) {
            friendService.connectMutually(cr.getSenderId(), cr.getRecipientId());

            AppUser recipient = appUserRepository.findById(cr.getRecipientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            notificationService.notify(
                    cr.getSenderId(),
                    "CONNECTION_REQUEST_ACCEPTED",
                    cr.getRecipientId(),
                    recipient.getDisplayName(),
                    recipient.getDisplayName() + " accepted your connection request.",
                    cr.getId(),
                    null);
        } else {
            // If this request was actually a circle-invite approval ask
            // (created by FriendService.sendApprovalRequest when the
            // contact's autoAcceptCircleInvites is false), declining it
            // should remove the dangling PENDING_APPROVAL friend_connection
            // row rather than leaving it stuck forever. This is a no-op for
            // ordinary second-degree "connect" requests, since those never
            // created a PENDING_APPROVAL row in the first place.
            friendService.cancelPendingApproval(cr.getSenderId(), cr.getRecipientId());
        }

        return toResult(cr);
    }

    private ConnectionRequestResult toResult(ConnectionRequest cr) {
        return new ConnectionRequestResult(cr.getId(), cr.getSenderId(), cr.getRecipientId(), cr.getStatus());
    }
}
