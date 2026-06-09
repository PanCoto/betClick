package com.betclick.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class SettleEventRequest {

    @NotNull(message = "Wynik gospodarza (drużyna A) jest wymagany")
    private Integer resultA;

    @NotNull(message = "Wynik gościa (drużyna B) jest wymagany")
    private Integer resultB;

    @NotNull(message = "Musisz wskazać wygrywające selekcje")
    private List<Long> winningSelectionIds;
}
