package com.betclick.repository;

import com.betclick.model.Selection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SelectionRepository extends JpaRepository<Selection, Long> {
    List<Selection> findByMarketIdAndIsActiveTrue(Long marketId);
    List<Selection> findByMarketId(Long marketId);
}
