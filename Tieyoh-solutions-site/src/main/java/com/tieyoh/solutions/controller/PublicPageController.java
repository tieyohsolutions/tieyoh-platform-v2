package com.tieyoh.solutions.controller;

import com.tieyoh.solutions.service.EventService;
import com.tieyoh.solutions.service.PublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PublicPageController {

    private final EventService eventService;
    private final PublicationService publicationService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/investors")
    public String investors() {
        return "investors";
    }

    @GetMapping("/events")
    public String events(Model model) {
        model.addAttribute("events", eventService.publicEvents());
        return "events";
    }

    @GetMapping("/publications")
    public String publications(Model model) {
        model.addAttribute("publications", publicationService.publicPublications());
        return "publications";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/careers")
    public String careers() {
        return "careers";
    }
}