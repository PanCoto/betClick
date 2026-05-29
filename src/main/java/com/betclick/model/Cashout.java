package com.betclick.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cashouts", uniqueConstraints = @UniqueConstraint(columnNames = "coupon_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cashout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_stake", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalStake;

    @Column(name = "potential_win", nullable = false, precision = 12, scale = 2)
    private BigDecimal potentialWin;

    @Column(name = "cashout_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal cashoutAmount;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
}
