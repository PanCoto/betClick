package com.betclick.repository;

import com.betclick.model.Cashout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashoutRepository extends JpaRepository<Cashout, Long> {
    boolean existsByCouponId(Long couponId);
    Optional<Cashout> findByCouponId(Long couponId);
}
