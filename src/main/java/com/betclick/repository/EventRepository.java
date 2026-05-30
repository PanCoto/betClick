package com.betclick.repository;

import com.betclick.model.Event;
import com.betclick.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStatusOrderByStartTimeAsc(EventStatus status);
    Page<Event> findByStatusOrderByStartTimeAsc(EventStatus status, Pageable pageable);
    List<Event> findByLeagueIdOrderByStartTimeAsc(Long leagueId);
    List<Event> findByLeagueIdAndStatusOrderByStartTimeAsc(Long leagueId, EventStatus status);
    long countByStatusIn(Collection<EventStatus> statuses);

    @Query("SELECT e FROM Event e JOIN e.league l WHERE l.sport.id = :sportId AND e.status = :status ORDER BY e.startTime ASC")
    Page<Event> findBySportIdAndStatusOrderByStartTimeAsc(@Param("sportId") Long sportId,
                                                           @Param("status") EventStatus status,
                                                           Pageable pageable);

    @Query(value = "SELECT * FROM events WHERE status IN ('UPCOMING', 'LIVE') " +
                   "AND (start_time + (expected_duration_minutes * INTERVAL '1 minute')) <= " +
                   "(CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Warsaw')",
           nativeQuery = true)
    List<Event> findEventsToAutoSettle();

    @Query("SELECT e FROM Event e JOIN FETCH e.league l JOIN FETCH l.sport WHERE e.id IN :ids")
    List<Event> findAllByIdWithLeagueSport(@Param("ids") Collection<Long> ids);

    @Query("SELECT e FROM Event e JOIN FETCH e.league l JOIN FETCH l.sport")
    List<Event> findAllWithLeagueSport();

    @Query(value = "SELECT e FROM Event e JOIN FETCH e.league l JOIN FETCH l.sport",
           countQuery = "SELECT count(e) FROM Event e")
    Page<Event> findAllWithLeagueSport(Pageable pageable);
}
