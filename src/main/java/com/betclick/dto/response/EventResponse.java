package com.betclick.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private String name;
    private String teamA;
    private String teamB;
    private LocalDateTime startTime;
    private Integer expectedDurationMinutes;
    private LocalDateTime endTime;
    private String status;
    private Integer resultA;
    private Integer resultB;
    private String leagueName;
    private String sportName;
    private boolean favorite;
    private List<MarketResponse> markets;
}
