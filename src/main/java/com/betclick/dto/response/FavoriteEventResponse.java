package com.betclick.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteEventResponse {
    private Long favoriteId;
    private Long eventId;
    private String eventName;
    private String teamA;
    private String teamB;
    private String sportName;
    private String leagueName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime createdAt;
}
