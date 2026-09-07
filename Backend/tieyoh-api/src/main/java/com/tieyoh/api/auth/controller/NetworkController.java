package com.tieyoh.api.auth.controller;

import com.tieyoh.api.auth.dto.FeedPersonResult;
import com.tieyoh.api.auth.service.FriendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Backs the feed's network section -- this user's circle plus their
// circle's circle (second-degree / friends of friends).
@RestController
@RequestMapping("/api/users/{userId}/network")
public class NetworkController {

    private final FriendService friendService;

    public NetworkController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public List<FeedPersonResult> getNetwork(@PathVariable Long userId) {
        return friendService.getNetwork(userId);
    }
}
