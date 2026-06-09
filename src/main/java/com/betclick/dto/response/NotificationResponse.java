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
public class NotificationResponse {
    private Long id;
    private String typeCode;
    private String statusCode;
    private String title;
    private String message;
    private String relatedEntityType;
    private Long relatedEntityId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
