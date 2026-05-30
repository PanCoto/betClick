package com.betclick.repository;

import com.betclick.model.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {
    Optional<UserLevel> findByCode(String code);
    Optional<UserLevel> findTopByMinTotalStakeLessThanEqualOrderBySortOrderDesc(BigDecimal totalStake);
}
