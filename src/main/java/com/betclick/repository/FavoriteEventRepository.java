package com.betclick.repository;

import com.betclick.model.FavoriteEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteEventRepository extends JpaRepository<FavoriteEvent, Long> {
    Optional<FavoriteEvent> findByUserIdAndEventId(Long userId, Long eventId);
    List<FavoriteEvent> findByUserIdAndStatusIdOrderByCreatedAtDesc(Long userId, Long statusId);

    @Query("SELECT f.event.id FROM FavoriteEvent f WHERE f.user.id = :userId AND f.statusId = :statusId")
    List<Long> findActiveEventIds(@Param("userId") Long userId, @Param("statusId") Long statusId);
}
