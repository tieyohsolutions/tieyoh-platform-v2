package com.tieyoh.api.auth.dto;

public class ConnectionRequestResult {

    private Long id;
    private Long senderId;
    private Long recipientId;
    private String status;

    public ConnectionRequestResult() {
    }

    public ConnectionRequestResult(Long id, Long senderId, Long recipientId, String status) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
