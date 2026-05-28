package com.betclick.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_levels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_total_won", nullable = false, precision = 12, scale = 2)
    private BigDecimal minTotalWon;

    @Column(name = "min_total_stake", nullable = false, precision = 12, scale = 2)
    private BigDecimal minTotalStake;

    @Column(name = "sort_order", nullable = false, unique = true)
    private Integer sortOrder;
}
