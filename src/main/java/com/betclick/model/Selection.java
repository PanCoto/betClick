package com.betclick.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "selections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Selection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal odds;

    @Column(name = "is_winner")
    private Boolean isWinner;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
