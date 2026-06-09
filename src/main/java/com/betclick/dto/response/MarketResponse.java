package com.betclick.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isSettled;
    private List<SelectionResponse> selections;
}
