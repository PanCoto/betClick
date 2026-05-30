package com.betclick.controller;

import com.betclick.dto.response.CouponResponse;
import com.betclick.dto.response.EventResponse;
import com.betclick.dto.response.TransactionResponse;
import com.betclick.dto.response.UserProfileResponse;
import com.betclick.service.BettingService;
import com.betclick.service.CouponActionService;
import com.betclick.service.EventService;
import com.betclick.service.FinancialService;
import com.betclick.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final EventService eventService;
    private final BettingService bettingService;
    private final FinancialService financialService;
    private final CouponActionService couponActionService;

    public UserController(UserService userService, EventService eventService,
                          BettingService bettingService, FinancialService financialService,
                          CouponActionService couponActionService) {
        this.userService = userService;
        this.eventService = eventService;
        this.bettingService = bettingService;
        this.financialService = financialService;
        this.couponActionService = couponActionService;
    }



    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Authentication authentication, Model model) {
        if (isEmployee(authentication)) {
            return "redirect:/admin/dashboard";
        }
        if (principal == null) {
            return "redirect:/login?error=true";
        }

        String login = principal.getName();
        UserProfileResponse profile;
        try {
            profile = userService.getUserProfile(login);
        } catch (UsernameNotFoundException | IllegalArgumentException e) {
            log.warn("Authenticated user {} could not open dashboard", login, e);
            return "redirect:/login?error=true";
        }

        List<CouponResponse> recentCoupons = List.of();
        try {
            recentCoupons = bettingService.getRecentUserCoupons(login, 10);
        } catch (Exception e) {
            log.warn("Could not load recent coupons for dashboard user {}", login, e);
        }

        List<TransactionResponse> recentTransactions = List.of();
        try {
            recentTransactions = financialService.getRecentTransactionHistory(login, 10);
        } catch (Exception e) {
            log.warn("Could not load recent transactions for dashboard user {}", login, e);
        }

        List<EventResponse> popularEvents = List.of();
        try {
            popularEvents = eventService.getPopularEvents(5);
        } catch (Exception e) {
            log.warn("Could not load popular events for dashboard user {}", login, e);
        }

        model.addAttribute("profile", profile);
        model.addAttribute("popularEvents", popularEvents);
        model.addAttribute("recentCoupons", recentCoupons);
        model.addAttribute("recentTransactions", recentTransactions);
        return "user/dashboard";
    }

    private boolean isEmployee(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority())
                                || "ROLE_MODERATOR".equals(authority.getAuthority()));
    }

    @GetMapping("/wallet")
    public String wallet(Principal principal, Model model) {
        String login = principal.getName();
        model.addAttribute("profile", userService.getUserProfile(login));
        model.addAttribute("transactions", financialService.getTransactionHistory(login));
        return "user/wallet";
    }

    @GetMapping("/history")
    public String history(Principal principal, Model model) {
        String login = principal.getName();
        model.addAttribute("profile", userService.getUserProfile(login));
        model.addAttribute("coupons", bettingService.getUserCoupons(login));
        return "user/history";
    }

    @GetMapping("/api/history")
    @ResponseBody
    @Operation(summary = "Paginowana historia kuponów użytkownika", description = "Zwraca historię obstawionych kuponów zalogowanego użytkownika z paginacją")
    public Page<CouponResponse> getUserCouponsPaginated(Principal principal,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        String login = principal.getName();
        Pageable pageable = PageRequest.of(page, size);
        return bettingService.getUserCouponsPage(login, pageable);
    }

    @GetMapping("/coupon/{id}")
    public String couponDetail(@PathVariable("id") Long id, Principal principal, Model model) {
        String login = principal.getName();
        try {
            CouponResponse coupon = bettingService.getCouponDetails(login, id);
            model.addAttribute("profile", userService.getUserProfile(login));
            model.addAttribute("coupon", coupon);
            return "user/coupon-detail";
        } catch (SecurityException e) {
            return "redirect:/dashboard?error=unauthorized";
        } catch (IllegalArgumentException e) {
            return "redirect:/history?error=notfound";
        }
    }

    @PostMapping("/coupon/{id}/cashout")
    public String cashoutCoupon(@PathVariable("id") Long id,
                                @RequestParam(defaultValue = "history") String redirectTo,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            couponActionService.cashout(principal.getName(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Cashout zostal zaakceptowany.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nie masz dostepu do tego kuponu.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.warn("Could not cash out coupon {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cashout nie powiodl sie. Sprobuj ponownie.");
        }
        if ("detail".equalsIgnoreCase(redirectTo)) {
            return "redirect:/coupon/" + id;
        }
        return "redirect:/history";
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
