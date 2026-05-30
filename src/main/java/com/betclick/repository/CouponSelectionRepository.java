package com.betclick.repository;

import com.betclick.model.CouponSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CouponSelectionRepository extends JpaRepository<CouponSelection, Long> {
    List<CouponSelection> findByCouponId(Long couponId);
}
