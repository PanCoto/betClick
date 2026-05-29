package com.betclick.model;

import com.betclick.model.enums.CouponStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "couponSelections")
@EqualsAndHashCode(exclude = "couponSelections")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 25)
    private String ticketNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stake;

    @Column(name = "total_odds", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalOdds;

    @Column(name = "potential_win", nullable = false, precision = 12, scale = 2)
    private BigDecimal potentialWin;

    @Column(name = "actual_win", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal actualWin = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    @Column(name = "placed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime placedAt = LocalDateTime.now();

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CouponSelection> couponSelections = new ArrayList<>();
}
