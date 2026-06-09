package com.betclick.dto.request;

import com.betclick.model.enums.WarOutcome;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarGameRequest {
    @NotNull
    private WarOutcome selectedOutcome;

    @NotNull
    @DecimalMin(value = "0.10", message = "Stawka musi byc dodatnia")
    private BigDecimal stake;
}
