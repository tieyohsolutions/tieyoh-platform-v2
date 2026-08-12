package com.tieyoh.solutions.controller;

import com.tieyoh.solutions.security.CustomUserDetails;
import com.tieyoh.solutions.service.DocumentService;
import com.tieyoh.solutions.service.EventService;
import com.tieyoh.solutions.service.PublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final DocumentService documentService;
    private final EventService eventService;
    private final PublicationService publicationService;

    @GetMapping("/member/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("user", principal.getUser());
        model.addAttribute("documentCount", documentService.listAll().size());
        model.addAttribute("eventCount", eventService.allEvents().size());
        model.addAttribute("publicationCount", publicationService.allPublications().size());
        return "member/dashboard";
    }

    @GetMapping("/member/documents")
    public String documents(Model model) {
        model.addAttribute("documents", documentService.listAll());
        return "member/documents";
    }

    @GetMapping("/member/events")
    public String events(Model model) {
        model.addAttribute("events", eventService.allEvents());
        return "member/events";
    }

    @GetMapping("/member/publications")
    public String publications(Model model) {
        model.addAttribute("publications", publicationService.allPublications());
        return "member/publications";
    }

    @GetMapping("/member/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("user", principal.getUser());
        return "member/profile";
    }
}
