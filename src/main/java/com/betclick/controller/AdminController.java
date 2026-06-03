package com.betclick.controller;

import com.betclick.dto.request.CreateEventRequest;
import com.betclick.dto.request.SettleEventRequest;
import com.betclick.dto.response.TurnoverReportRow;
import com.betclick.dto.response.UserProfileResponse;
import com.betclick.model.AuditLog;
import com.betclick.model.enums.UserRole;
import com.betclick.service.AdminService;
import com.betclick.service.EventService;
import com.betclick.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private static final List<Integer> USER_PAGE_SIZES = List.of(20, 50, 100);

    private final AdminService adminService;
    private final UserService userService;
    private final EventService eventService;

    public AdminController(AdminService adminService, UserService userService, EventService eventService) {
        this.adminService = adminService;
        this.userService = userService;
        this.eventService = eventService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<AuditLog> logs = adminService.getAuditLogs();
        long playerCount = userService.countPlayers();
        long activeEventCount = eventService.countActiveEvents();

        model.addAttribute("logs", logs);
        model.addAttribute("playerCount", playerCount);
        model.addAttribute("activeEventCount", activeEventCount);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "50") int size,
                            Model model) {
        int safeSize = normalizeUserPageSize(size);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Page<com.betclick.model.User> usersPage = userService.findUsersPage(pageable);
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("userPageSizes", USER_PAGE_SIZES);
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-active")
    public String toggleUserActive(@PathVariable("id") Long id,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size,
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "Status konta użytkownika został pomyślnie zmieniony.");
        } catch (Exception e) {
            log.warn("Could not toggle active status for user {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Nie udalo sie zmienic statusu konta. Sprobuj ponownie pozniej.");
        }
        return usersRedirect(page, size);
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable("id") Long id,
                                 @RequestParam("role") UserRole role,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "50") int size,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.changeUserRole(id, role);
            redirectAttributes.addFlashAttribute("successMessage", "Rola użytkownika została pomyślnie zmieniona.");
        } catch (Exception e) {
            log.warn("Could not change role for user {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Nie udalo sie zmienic roli uzytkownika. Sprobuj ponownie pozniej.");
        }
        return usersRedirect(page, size);
    }

    @GetMapping("/events")
    public String listEvents(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeGenericPageSize(size));
        Page<com.betclick.dto.response.EventResponse> eventsPage = eventService.findAllEvents(pageable);
        if (!model.containsAttribute("createEventRequest")) {
            model.addAttribute("createEventRequest", new CreateEventRequest());
        }
        model.addAttribute("eventsPage", eventsPage);
        model.addAttribute("leagues", eventService.findAllLeagues());
        return "admin/events";
    }

    @PostMapping("/events")
    public String createEvent(@Valid @ModelAttribute("createEventRequest") CreateEventRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nie dodano wydarzenia. Sprawdź wymagane pola, czas trwania i kursy.");
            redirectAttributes.addFlashAttribute("createEventRequest", request);
            return "redirect:/admin/events";
        }

        try {
            eventService.createEvent(request);
            redirectAttributes.addFlashAttribute("successMessage", "Wydarzenie zostało dodane wraz z rynkiem Zwycięzca meczu.");
        } catch (Exception e) {
            log.warn("Could not create event {}", request.getName(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Nie udalo sie dodac wydarzenia. Sprawdz dane i sprobuj ponownie.");
            redirectAttributes.addFlashAttribute("createEventRequest", request);
        }
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/settle")
    public String settleEvent(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("settleRequest") SettleEventRequest request,
                              BindingResult bindingResult,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Błędne dane rozliczenia! Wszystkie pola są wymagane.");
            return eventsRedirect(page, size);
        }

        try {
            adminService.settleEvent(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Wydarzenie zostało pomyślnie rozliczone, a wygrane kupony zostały opłacone.");
        } catch (Exception e) {
            log.warn("Could not settle event {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Rozliczenie wydarzenia nie powiodlo sie. Sprobuj ponownie pozniej.");
        }
        return eventsRedirect(page, size);
    }

    @GetMapping("/reports")
    public String reportPage(@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                             Model model) {
        if (from == null) {
            from = LocalDate.now().minusMonths(1);
        }
        if (to == null) {
            to = LocalDate.now();
        }

        List<TurnoverReportRow> report = adminService.getTurnoverReport(from, to);
        model.addAttribute("report", report);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        return "admin/reports";
    }
    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("profile")
    public UserProfileResponse profile(Principal principal) {
        if (principal == null) {
            return null;
        }

        try {
            return userService.getUserProfile(principal.getName());
        } catch (Exception e) {
            log.warn("Could not load profile for admin header user {}", principal.getName(), e);
            return null;
        }
    }

    private int normalizeUserPageSize(int size) {
        return USER_PAGE_SIZES.contains(size) ? size : 50;
    }

    private String usersRedirect(int page, int size) {
        return "redirect:/admin/users?page=" + Math.max(page, 0) + "&size=" + normalizeUserPageSize(size);
    }

    private String eventsRedirect(int page, int size) {
        return "redirect:/admin/events?page=" + Math.max(page, 0) + "&size=" + normalizeGenericPageSize(size);
    }

    private int normalizeGenericPageSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }
}
