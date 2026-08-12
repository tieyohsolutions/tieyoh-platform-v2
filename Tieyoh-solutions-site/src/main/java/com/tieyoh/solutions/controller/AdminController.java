package com.tieyoh.solutions.controller;

import com.tieyoh.solutions.domain.Visibility;
import com.tieyoh.solutions.security.CustomUserDetails;
import com.tieyoh.solutions.service.DocumentService;
import com.tieyoh.solutions.service.EventService;
import com.tieyoh.solutions.service.PublicationService;
import com.tieyoh.solutions.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final DocumentService documentService;
    private final EventService eventService;
    private final PublicationService publicationService;

    @GetMapping("/members")
    public String members(Model model) {
        model.addAttribute("pending", userService.pendingApprovals());
        model.addAttribute("allUsers", userService.allUsers());
        return "admin/members";
    }

    @PostMapping("/members/{id}/approve")
    public String approve(@PathVariable Long id) {
        userService.approve(id);
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/reject")
    public String reject(@PathVariable Long id) {
        userService.reject(id);
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/disable")
    public String disable(@PathVariable Long id) {
        userService.disable(id);
        return "redirect:/admin/members";
    }

    @PostMapping("/members/invite")
    public String invite(@RequestParam String name, @RequestParam String email,
                          @RequestParam(required = false) String company,
                          @RequestParam(required = false) String title,
                          Model model) {
        String tempPassword = userService.inviteMember(name, email, company, title);
        model.addAttribute("pending", userService.pendingApprovals());
        model.addAttribute("allUsers", userService.allUsers());
        model.addAttribute("invitedEmail", email);
        model.addAttribute("tempPassword", tempPassword);
        return "admin/members";
    }

    @GetMapping("/documents")
    public String documents(Model model) {
        model.addAttribute("documents", documentService.listAll());
        return "admin/documents";
    }

    @PostMapping("/documents")
    public String uploadDocument(@RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String externalUrl,
                                  @RequestParam(required = false) MultipartFile file,
                                  @AuthenticationPrincipal CustomUserDetails principal) throws IOException {
        documentService.save(title, description, category, externalUrl, file, principal.getUser().getId());
        return "redirect:/admin/documents";
    }

    @PostMapping("/documents/{id}/delete")
    public String deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return "redirect:/admin/documents";
    }

    @GetMapping("/events")
    public String events(Model model) {
        model.addAttribute("events", eventService.allEvents());
        return "admin/events";
    }

    @PostMapping("/events")
    public String createEvent(@RequestParam String title,
                               @RequestParam(required = false) String description,
                               @RequestParam String eventDate,
                               @RequestParam(required = false) String location,
                               @RequestParam Visibility visibility,
                               @AuthenticationPrincipal CustomUserDetails principal) {
        eventService.save(title, description, LocalDateTime.parse(eventDate), location, visibility,
                principal.getUser().getId());
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id) {
        eventService.delete(id);
        return "redirect:/admin/events";
    }

    @GetMapping("/publications")
    public String publications(Model model) {
        model.addAttribute("publications", publicationService.allPublications());
        return "admin/publications";
    }

    @PostMapping("/publications")
    public String createPublication(@RequestParam String title,
                                     @RequestParam(required = false) String summary,
                                     @RequestParam(required = false) String content,
                                     @RequestParam(required = false) String externalUrl,
                                     @RequestParam Visibility visibility,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        publicationService.save(title, summary, content, externalUrl, visibility, principal.getUser().getId());
        return "redirect:/admin/publications";
    }

    @PostMapping("/publications/{id}/delete")
    public String deletePublication(@PathVariable Long id) {
        publicationService.delete(id);
        return "redirect:/admin/publications";
    }
}
