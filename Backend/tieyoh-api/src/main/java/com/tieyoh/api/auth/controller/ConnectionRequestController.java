package com.tieyoh.api.auth.controller;

import com.tieyoh.api.auth.dto.ConnectionRequestBody;
import com.tieyoh.api.auth.dto.ConnectionRequestRespondBody;
import com.tieyoh.api.auth.dto.ConnectionRequestResult;
import com.tieyoh.api.auth.service.ConnectionRequestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/connection-requests")
public class ConnectionRequestController {

    private final ConnectionRequestService connectionRequestService;

    public ConnectionRequestController(ConnectionRequestService connectionRequestService) {
        this.connectionRequestService = connectionRequestService;
    }

    @PostMapping
    public ConnectionRequestResult sendRequest(@RequestBody ConnectionRequestBody body) {
        return connectionRequestService.sendRequest(body.getSenderId(), body.getRecipientId());
    }

    @PostMapping("/{id}/respond")
    public ConnectionRequestResult respond(
            @PathVariable Long id,
            @RequestBody ConnectionRequestRespondBody body) {
        return connectionRequestService.respond(id, body.isAccept());
    }
}
