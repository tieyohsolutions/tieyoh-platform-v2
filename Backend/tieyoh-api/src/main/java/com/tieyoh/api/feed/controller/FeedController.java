package com.tieyoh.api.feed.controller;

import com.tieyoh.api.feed.dto.FeedResponse;
import com.tieyoh.api.feed.service.FeedService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    // viewerId is a plain request param, not a path variable, to match
    // the spec's suggested "GET /api/feed?page=0&size=20" shape as
    // closely as this backend's client-trusted-id model allows.
    @GetMapping
    public FeedResponse getFeed(
            @RequestParam Long viewerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return feedService.getFeed(viewerId, page, size);
    }
}
