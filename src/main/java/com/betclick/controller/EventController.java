package com.betclick.controller;

import com.betclick.dto.response.EventResponse;
import com.betclick.dto.response.UserProfileResponse;
import com.betclick.exception.EventNotFoundException;
import com.betclick.service.EventService;
import com.betclick.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

@Controller
@Tag(name = "Wydarzenia", description = "Informacje o wydarzeniach sportowych")
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    public EventController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    @GetMapping("/events")
    public String listEvents(@RequestParam(value = "sportId", required = false) Long sportId,
                             Principal principal,
                             Model model) {
        String login = principal.getName();
        UserProfileResponse profile = userService.getUserProfile(login);

        List<EventResponse> events;
        if (sportId != null) {
            events = eventService.findUpcomingEventsBySport(sportId);
            model.addAttribute("selectedSportId", sportId);
        } else {
            events = eventService.findUpcomingEvents();
            model.addAttribute("selectedSportId", null);
        }

        model.addAttribute("profile", profile);
        model.addAttribute("sports", eventService.findAllActiveSports());
        model.addAttribute("events", events);
        return "user/events";
    }

    @GetMapping("/api/events")
    @ResponseBody
    @Operation(summary = "Lista wydarzeń z paginacją", description = "Zwraca paginowaną listę zaplanowanych wydarzeń, opcjonalnie przefiltrowaną po sportId")
    public ResponseEntity<Page<EventResponse>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long sportId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponse> responsePage;
        if (sportId != null) {
            responsePage = eventService.findUpcomingEventsBySport(sportId, pageable);
        } else {
            responsePage = eventService.findUpcomingEvents(pageable);
        }
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/api/events/{id}")
    @ResponseBody
    @Operation(summary = "Pobierz szczegóły wydarzenia", description = "Zwraca pełne informacje o pojedynczym wydarzeniu wraz z rynkami i selekcjami")
    public ResponseEntity<EventResponse> getEventDetails(@PathVariable("id") Long id) {
        try {
            EventResponse event = eventService.getEventDetails(id);
            return ResponseEntity.ok(event);
        } catch (EventNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @ModelAttribute("currentUri")
public String currentUri(HttpServletRequest request) {
    return request.getRequestURI();
}
}
