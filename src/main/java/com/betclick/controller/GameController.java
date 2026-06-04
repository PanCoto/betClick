package com.betclick.controller;

import com.betclick.dto.request.WarGameRequest;
import com.betclick.dto.response.WarGameRoundResponse;
import com.betclick.exception.InsufficientFundsException;
import com.betclick.model.enums.WarOutcome;
import com.betclick.service.UserService;
import com.betclick.service.WarGameService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/games")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final WarGameService warGameService;
    private final UserService userService;

    public GameController(WarGameService warGameService, UserService userService) {
        this.warGameService = warGameService;
        this.userService = userService;
    }

    @GetMapping
    public String games() {
        return "redirect:/games/war";
    }

    @GetMapping("/war")
    public String war(Principal principal, Model model) {
        String login = principal.getName();
        if (!model.containsAttribute("warGameRequest")) {
            WarGameRequest request = new WarGameRequest();
            request.setSelectedOutcome(WarOutcome.PLAYER_WIN);
            model.addAttribute("warGameRequest", request);
        }
        model.addAttribute("profile", userService.getUserProfile(login));
        model.addAttribute("history", warGameService.history(login));
        model.addAttribute("outcomes", WarOutcome.values());
        return "games/war";
    }

    @PostMapping("/war")
    public String playWar(@Valid @ModelAttribute("warGameRequest") WarGameRequest request,
                          BindingResult bindingResult,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("warError", "Podaj poprawny typ i dodatnia stawke.");
            redirectAttributes.addFlashAttribute("warGameRequest", request);
            return "redirect:/games/war";
        }

        try {
            WarGameRoundResponse round = warGameService.play(principal.getName(), request);
            redirectAttributes.addFlashAttribute("lastRound", round);
            redirectAttributes.addFlashAttribute("warSuccess", "Runda Wojny zostala rozliczona.");
        } catch (InsufficientFundsException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("warError", e.getMessage());
            redirectAttributes.addFlashAttribute("warGameRequest", request);
        } catch (Exception e) {
            log.warn("Could not play war game for user {}", principal.getName(), e);
            redirectAttributes.addFlashAttribute("warError", "Nie udalo sie rozegrac rundy. Sprobuj ponownie.");
            redirectAttributes.addFlashAttribute("warGameRequest", request);
        }
        return "redirect:/games/war";
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
