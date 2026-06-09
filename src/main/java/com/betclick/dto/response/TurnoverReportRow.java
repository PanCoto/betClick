package com.betclick.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoverReportRow {
    private LocalDate reportDate;
    private BigDecimal deposits;
    private BigDecimal withdrawals;
    private BigDecimal betsPlaced;
    private BigDecimal winsPaid;
    private BigDecimal houseProfit;
}
