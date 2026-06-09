package com.betclick.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarGameRoundResponse {
    private Long id;
    private BigDecimal stake;
    private String selectedOutcome;
    private String actualOutcome;
    private BigDecimal odds;
    private BigDecimal payout;
    private String statusCode;
    private Integer playerCard;
    private Integer dealerCard;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
}
