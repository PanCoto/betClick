package com.betclick.controller;

import com.betclick.dto.request.DepositWithdrawRequest;
import com.betclick.exception.InsufficientFundsException;
import com.betclick.service.FinancialService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/financial")
public class FinancialController {

    private static final Logger log = LoggerFactory.getLogger(FinancialController.class);

    private final FinancialService financialService;

    public FinancialController(FinancialService financialService) {
        this.financialService = financialService;
    }

    @PostMapping("/deposit")
    public String deposit(@Valid @ModelAttribute("depositRequest") DepositWithdrawRequest request,
                          BindingResult result,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("depositError", "Błędna kwota wpłaty!");
            return "redirect:/wallet";
        }

        try {
            String login = principal.getName();
            financialService.deposit(login, request.getAmount());
            redirectAttributes.addFlashAttribute("depositSuccess", "Wpłata zakończona sukcesem!");
        } catch (Exception e) {
            log.warn("Could not deposit funds", e);
            redirectAttributes.addFlashAttribute("depositError", "Wplata nie powiodla sie. Sprobuj ponownie pozniej.");
        }

        return "redirect:/wallet";
    }

    @PostMapping("/withdraw")
    public String withdraw(@Valid @ModelAttribute("withdrawRequest") DepositWithdrawRequest request,
                           BindingResult result,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("withdrawError", "Błędna kwota wypłaty!");
            return "redirect:/wallet";
        }

        try {
            String login = principal.getName();
            financialService.withdraw(login, request.getAmount());
            redirectAttributes.addFlashAttribute("withdrawSuccess", "Wypłata zakończona sukcesem!");
        } catch (InsufficientFundsException e) {
            redirectAttributes.addFlashAttribute("withdrawError", "Niewystarczające środki na koncie!");
        } catch (Exception e) {
            log.warn("Could not withdraw funds", e);
            redirectAttributes.addFlashAttribute("withdrawError", "Wyplata nie powiodla sie. Sprobuj ponownie pozniej.");
        }

        return "redirect:/wallet";
    }
    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
