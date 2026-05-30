package com.betclick.repository;

import com.betclick.model.Bonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BonusRepository extends JpaRepository<Bonus, Long> {
    boolean existsByUserIdAndBonusTypeId(Long userId, Long bonusTypeId);
    List<Bonus> findByUserIdOrderByCreatedAtDesc(Long userId);
}
