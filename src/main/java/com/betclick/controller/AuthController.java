package com.betclick.controller;

import com.betclick.dto.request.LoginRequest;
import com.betclick.dto.request.RegisterRequest;
import com.betclick.dto.response.LoginResponse;
import com.betclick.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Tag(name = "Autoryzacja", description = "Logowanie i rejestracja")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Błędny login lub hasło!");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Pomyślnie wylogowano!");
        }
        if (registered != null) {
            model.addAttribute("registrationSuccess", "Rejestracja zakończona sukcesem! Możesz się zalogować.");
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    @Operation(summary = "Zaloguj się", description = "Autoryzuje użytkownika i ustawia ciasteczko JWT")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest request,
                        BindingResult result,
                        HttpServletResponse response,
                        Model model) {
        if (result.hasErrors()) {
            return "auth/login";
        }

        try {
            LoginResponse loginResponse = authService.login(request);
            
            Cookie jwtCookie = new Cookie("jwt_token", loginResponse.getToken());
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60);
            response.addCookie(jwtCookie);

            if ("ADMIN".equals(loginResponse.getRole()) || "MODERATOR".equals(loginResponse.getRole())) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.warn("Login failed for user {}", request.getLogin(), e);
            model.addAttribute("loginError", "Błędne dane logowania lub konto jest zablokowane!");
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    @Operation(summary = "Zarejestruj się", description = "Tworzy nowe konto gracza")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult result,
                           Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(request);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("registerError", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("registerError", "Wystąpił nieoczekiwany błąd podczas rejestracji!");
            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);
        return "redirect:/login?logout=true";
    }
}
