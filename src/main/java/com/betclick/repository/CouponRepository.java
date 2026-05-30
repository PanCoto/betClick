package com.betclick.repository;

import com.betclick.model.Coupon;
import com.betclick.model.enums.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    List<Coupon> findByUserIdOrderByPlacedAtDesc(Long userId);
    Page<Coupon> findByUserIdOrderByPlacedAtDesc(Long userId, Pageable pageable);
    List<Coupon> findByUserIdAndStatusOrderByPlacedAtDesc(Long userId, CouponStatus status);
    List<Coupon> findByUserIdAndStatusOrderByPlacedAtDesc(Long userId, CouponStatus status, Pageable pageable);
    Optional<Coupon> findByTicketNumber(String ticketNumber);
    List<Coupon> findAllByOrderByPlacedAtDesc();
    List<Coupon> findAllByOrderByPlacedAtDesc(Pageable pageable);
}
