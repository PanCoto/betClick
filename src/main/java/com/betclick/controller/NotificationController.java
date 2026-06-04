package com.betclick.controller;

import com.betclick.service.NotificationService;
import com.betclick.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/user/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public String notifications(Principal principal, Model model) {
        String login = principal.getName();
        model.addAttribute("profile", userService.getUserProfile(login));
        model.addAttribute("notifications", notificationService.getNotifications(login));
        return "user/notifications";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable("id") Long id,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(principal.getName(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Powiadomienie oznaczone jako przeczytane.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nie masz dostepu do tego powiadomienia.");
        } catch (Exception e) {
            log.warn("Could not mark notification {} as read", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Nie udalo sie oznaczyc powiadomienia.");
        }
        return "redirect:/user/notifications";
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
