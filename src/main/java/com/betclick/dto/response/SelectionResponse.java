package com.betclick.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectionResponse {
    private Long id;
    private String name;
    private BigDecimal odds;
    private Boolean isWinner;
    private Boolean isActive;
}
