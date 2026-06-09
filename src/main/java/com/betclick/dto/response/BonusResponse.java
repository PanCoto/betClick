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
public class BonusResponse {
    private Long id;
    private String typeCode;
    private String statusCode;
    private BigDecimal amount;
    private BigDecimal remainingAmount;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;
}
