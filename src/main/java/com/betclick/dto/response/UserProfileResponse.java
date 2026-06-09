package com.betclick.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private String login;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private BigDecimal balance;
    private LocalDateTime registrationDate;
    private String role;
    private String levelCode;
    private String levelName;
    private BigDecimal availableBonusAmount;
    private Long unreadNotificationsCount;

    private Long totalBets;
    private Long wonBets;
    private Long lostBets;
    private Long activeBets;
    private BigDecimal totalStaked;
    private BigDecimal totalWon;
    private BigDecimal winRate;
}
