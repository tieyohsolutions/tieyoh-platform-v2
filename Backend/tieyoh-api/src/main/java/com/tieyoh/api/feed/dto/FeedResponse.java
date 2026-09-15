package com.tieyoh.api.feed.dto;

import java.util.List;

public class FeedResponse {

    private List<FeedItemResult> items;
    private int page;
    private int size;
    private boolean hasMore;

    public FeedResponse() {
    }

    public FeedResponse(List<FeedItemResult> items, int page, int size, boolean hasMore) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.hasMore = hasMore;
    }

    public List<FeedItemResult> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public boolean isHasMore() {
        return hasMore;
    }
}
