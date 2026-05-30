package com.betclick.repository;

import com.betclick.model.WarGameRound;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarGameRoundRepository extends JpaRepository<WarGameRound, Long> {
    List<WarGameRound> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
