package com.betclick.controller;

import com.betclick.dto.request.PlaceBetRequest;
import com.betclick.dto.response.CouponResponse;
import com.betclick.service.BettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/bets")
@Tag(name = "Zakłady", description = "Operacje związane z obstawianiem kuponów")
public class BettingController {

    private final BettingService bettingService;

    public BettingController(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    @PostMapping
    @Operation(summary = "Złóż zakład", description = "Tworzy nowy kupon dla zalogowanego użytkownika")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Zakład przyjęty"),
        @ApiResponse(responseCode = "400", description = "Błędne dane lub niewystarczające środki"),
        @ApiResponse(responseCode = "401", description = "Brak autoryzacji")
    })
    public ResponseEntity<CouponResponse> placeBet(@Valid @RequestBody PlaceBetRequest request, Principal principal) {
        String login = principal.getName();
        CouponResponse coupon = bettingService.placeBet(login, request);
        return ResponseEntity.ok(coupon);
    }
}
