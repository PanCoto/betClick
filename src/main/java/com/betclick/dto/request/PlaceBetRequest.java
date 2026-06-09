package com.betclick.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PlaceBetRequest {

    @NotEmpty(message = "Musisz wybrać co najmniej jedno wydarzenie")
    private List<Long> selectionIds;

    @NotNull(message = "Stawka jest wymagana")
    @DecimalMin(value = "0.10", message = "Minimalna stawka to 0.10 PLN")
    private BigDecimal stake;
}
