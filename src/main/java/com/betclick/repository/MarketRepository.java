package com.betclick.repository;

import com.betclick.model.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {
    List<Market> findByEventIdAndIsActiveTrue(Long eventId);
    List<Market> findByEventId(Long eventId);
}
