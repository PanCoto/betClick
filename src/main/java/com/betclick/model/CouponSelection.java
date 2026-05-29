package com.betclick.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "coupon_selections",
       uniqueConstraints = @UniqueConstraint(columnNames = {"coupon_id", "selection_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "coupon")
@EqualsAndHashCode(exclude = "coupon")
public class CouponSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selection_id", nullable = false)
    private Selection selection;

    @Column(name = "odds_at_placement", nullable = false, precision = 6, scale = 2)
    private BigDecimal oddsAtPlacement;
}
