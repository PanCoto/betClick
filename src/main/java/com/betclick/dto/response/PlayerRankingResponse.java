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
public class PlayerRankingResponse {
    private int place;
    private String login;
    private BigDecimal totalStaked;
    private BigDecimal totalWon;
    private BigDecimal totalProfit;
    private Long wonCouponsCount;
    private Long lostCouponsCount;
    private Long gamesWonCount;
    private Long gamesLostCount;
    private LocalDateTime updatedAt;
}
