package com.betclick.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositWithdrawRequest {

    @NotNull(message = "Kwota jest wymagana")
    @DecimalMin(value = "1.00", message = "Minimalna kwota to 1.00 PLN")
    private BigDecimal amount;
}
