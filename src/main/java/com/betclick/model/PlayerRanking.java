package com.betclick.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_rankings", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_staked", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalStaked = BigDecimal.ZERO;

    @Column(name = "total_won", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalWon = BigDecimal.ZERO;

    @Column(name = "total_profit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalProfit = BigDecimal.ZERO;

    @Column(name = "won_coupons_count", nullable = false)
    @Builder.Default
    private Long wonCouponsCount = 0L;

    @Column(name = "lost_coupons_count", nullable = false)
    @Builder.Default
    private Long lostCouponsCount = 0L;

    @Column(name = "games_won_count", nullable = false)
    @Builder.Default
    private Long gamesWonCount = 0L;

    @Column(name = "games_lost_count", nullable = false)
    @Builder.Default
    private Long gamesLostCount = 0L;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
