package com.tieyoh.api.auth.controller;

import com.tieyoh.api.auth.dto.AddFriendRequest;
import com.tieyoh.api.auth.dto.ContactMatchBatchRequest;
import com.tieyoh.api.auth.dto.ContactMatchResult;
import com.tieyoh.api.auth.dto.FriendResult;
import com.tieyoh.api.auth.service.FriendService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Backs the "circle" behind the add-people icon and the send-gift flow --
// a persistent list, separate from UserProfileService's one-off search.
// Keyed in the URL by the owner's Tieyoh @t- id (e.g. "@t-thg"), not an
// internal numeric id -- see FriendConnection's class comment for why.
@RestController
@RequestMapping("/api/users/{ownerTieyohId}/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public List<FriendResult> getFriends(@PathVariable String ownerTieyohId) {
        return friendService.getFriends(ownerTieyohId);
    }

    @PostMapping
    public FriendResult addFriend(
            @PathVariable String ownerTieyohId,
            @Valid @RequestBody AddFriendRequest request) {
        return friendService.addFriend(ownerTieyohId, request);
    }

    // Read-only: classifies a batch of phone-book contacts as already on
    // Tieyoh (with basic profile info + mutual-friends count) or not,
    // without creating any circle/invitation rows. Backs the "People you
    // may know" onboarding screen -- Connect and Invite taps on that
    // screen call the real addFriend / invitation-batch endpoints
    // separately, once the user actually acts on a row.
    @PostMapping("/check-contacts")
    public List<ContactMatchResult> checkContacts(
            @PathVariable String ownerTieyohId,
            @RequestBody ContactMatchBatchRequest request) {
        return friendService.checkContacts(ownerTieyohId, request.getContacts());
    }

}
