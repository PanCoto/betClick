package com.betclick.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private Long id;
    private String ticketNumber;
    private BigDecimal stake;
    private BigDecimal totalOdds;
    private BigDecimal potentialWin;
    private BigDecimal actualWin;
    private String status;
    private BigDecimal cashoutAmount;
    private boolean cashoutAvailable;
    private boolean cancellationAvailable;
    private LocalDateTime placedAt;
    private LocalDateTime settledAt;
    private List<SelectionDetail> selections;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectionDetail {
        private Long selectionId;
        private String selectionName;
        private String marketName;
        private String eventName;
        private LocalDateTime eventStartTime;
        private Integer eventExpectedDurationMinutes;
        private LocalDateTime eventEndTime;
        private BigDecimal oddsAtPlacement;
        private Boolean isWinner;
    }
}
