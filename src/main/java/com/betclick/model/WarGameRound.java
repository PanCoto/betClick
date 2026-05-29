package com.betclick.model;

import com.betclick.model.enums.WarOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "war_game_rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarGameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_type_id")
    private Long gameTypeId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stake;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_outcome", nullable = false, length = 40)
    private WarOutcome selectedOutcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_outcome", nullable = false, length = 40)
    private WarOutcome actualOutcome;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal odds;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal payout = BigDecimal.ZERO;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "player_card", nullable = false)
    private Integer playerCard;

    @Column(name = "dealer_card", nullable = false)
    private Integer dealerCard;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "settled_at")
    private LocalDateTime settledAt;
}
