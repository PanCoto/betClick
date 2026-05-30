package com.betclick.repository;

import com.betclick.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SportRepository extends JpaRepository<Sport, Long> {
    List<Sport> findAllByIsActiveTrue();
    Optional<Sport> findByName(String name);
}
