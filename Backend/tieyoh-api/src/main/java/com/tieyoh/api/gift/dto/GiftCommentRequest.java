package com.tieyoh.api.gift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// authorUserId is trusted straight from the client, same trust model as
// the rest of this backend -- see GiftRequest's own comment on this.
public class GiftCommentRequest {

    @NotNull(message = "authorUserId is required")
    private Long authorUserId;

    @NotBlank(message = "body is required")
    private String body;

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(Long authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
