package com.betclick.repository;

import com.betclick.model.PlayerRanking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRankingRepository extends JpaRepository<PlayerRanking, Long> {
    Optional<PlayerRanking> findByUserId(Long userId);
    List<PlayerRanking> findByOrderByTotalWonDescTotalProfitDesc(Pageable pageable);
}
