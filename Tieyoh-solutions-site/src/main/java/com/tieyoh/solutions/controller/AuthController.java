package com.tieyoh.solutions.controller;

import com.tieyoh.solutions.dto.SignupForm;
import com.tieyoh.solutions.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/auth/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/auth/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "auth/signup";
    }

    @PostMapping("/auth/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "auth/signup";
        }
        try {
            userService.registerSelfSignup(form.getName(), form.getEmail(), form.getCompany(),
                    form.getTitle(), form.getPassword());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("signupError", ex.getMessage());
            return "auth/signup";
        }
        return "auth/pending";
    }
}
