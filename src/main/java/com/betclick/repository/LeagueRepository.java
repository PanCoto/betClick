package com.betclick.repository;

import com.betclick.model.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LeagueRepository extends JpaRepository<League, Long> {
    List<League> findAllByIsActiveTrue();
    List<League> findBySportIdAndIsActiveTrue(Long sportId);
    @Query("SELECT l FROM League l JOIN FETCH l.sport")
    List<League> findAllWithSport();
}