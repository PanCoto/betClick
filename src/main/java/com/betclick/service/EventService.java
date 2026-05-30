package com.betclick.service;

import com.betclick.dto.request.CreateEventRequest;
import com.betclick.dto.response.EventResponse;
import com.betclick.dto.response.MarketResponse;
import com.betclick.dto.response.SelectionResponse;
import com.betclick.exception.EventNotFoundException;
import com.betclick.model.*;
import com.betclick.model.enums.EventStatus;
import com.betclick.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final int DEFAULT_EVENT_LIMIT = 20;

    private final SportRepository sportRepository;
    private final LeagueRepository leagueRepository;
    private final EventRepository eventRepository;
    private final MarketRepository marketRepository;
    private final SelectionRepository selectionRepository;
    private final JdbcTemplate jdbcTemplate;

    public EventService(SportRepository sportRepository, LeagueRepository leagueRepository,
                        EventRepository eventRepository, MarketRepository marketRepository,
                        SelectionRepository selectionRepository, JdbcTemplate jdbcTemplate) {
        this.sportRepository = sportRepository;
        this.leagueRepository = leagueRepository;
        this.eventRepository = eventRepository;
        this.marketRepository = marketRepository;
        this.selectionRepository = selectionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<Sport> findAllActiveSports() {
        return sportRepository.findAllByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Sport> findAllSports() {
        return sportRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<League> findLeaguesBySport(Long sportId) {
        return leagueRepository.findBySportIdAndIsActiveTrue(sportId);
    }

    @Transactional(readOnly = true)
    public List<League> findAllLeagues() {
        return leagueRepository.findAllWithSport();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findUpcomingEvents() {
        Page<Event> upcoming = eventRepository.findByStatusOrderByStartTimeAsc(
                EventStatus.UPCOMING,
                PageRequest.of(0, DEFAULT_EVENT_LIMIT)
        );
        return upcoming.getContent().stream().map(this::mapToEventResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findUpcomingEventsBySport(Long sportId) {
        Page<Event> upcoming = eventRepository.findBySportIdAndStatusOrderByStartTimeAsc(
                sportId,
                EventStatus.UPCOMING,
                PageRequest.of(0, DEFAULT_EVENT_LIMIT)
        );
        return upcoming.getContent().stream().map(this::mapToEventResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getPopularEvents(int limit) {
        String sql = "SELECT event_id FROM get_top_events(?)";
        int safeLimit = safeSize(limit);
        try {
            List<Long> eventIds = jdbcTemplate.queryForList(sql, Long.class, safeLimit);
            if (eventIds.isEmpty()) {
                return eventRepository.findByStatusOrderByStartTimeAsc(EventStatus.UPCOMING, PageRequest.of(0, safeLimit)).getContent().stream()
                        .map(this::mapToEventResponse)
                        .collect(Collectors.toList());
            }
            List<Event> events = eventRepository.findAllById(eventIds);
            return eventIds.stream()
                    .map(id -> events.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .map(this::mapToEventResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not load popular events from database function", e);
            return eventRepository.findByStatusOrderByStartTimeAsc(EventStatus.UPCOMING, PageRequest.of(0, safeLimit)).getContent().stream()
                    .map(this::mapToEventResponse)
                    .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findUpcomingEvents(Pageable pageable) {
        return eventRepository.findByStatusOrderByStartTimeAsc(EventStatus.UPCOMING, pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findUpcomingEventsBySport(Long sportId, Pageable pageable) {
        return eventRepository.findBySportIdAndStatusOrderByStartTimeAsc(sportId, EventStatus.UPCOMING, pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional
    public Event createEvent(CreateEventRequest request) {
        League league = leagueRepository.findById(request.getLeagueId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono ligi o ID " + request.getLeagueId()));

        Event event = Event.builder()
                .league(league)
                .name(request.getName())
                .teamA(request.getTeamA())
                .teamB(request.getTeamB())
                .startTime(request.getStartTime())
                .expectedDurationMinutes(request.getExpectedDurationMinutes())
                .status(EventStatus.UPCOMING)
                .build();

        event = eventRepository.save(event);

        boolean isDrawPossible = !league.getSport().getName().equalsIgnoreCase("Tenis") &&
                                 !league.getSport().getName().equalsIgnoreCase("Boks") &&
                                 !league.getSport().getName().equalsIgnoreCase("MMA");

        Market winnerMarket = Market.builder()
                .event(event)
                .name("Zwycięzca meczu")
                .description("Typowanie wyniku końcowego spotkania")
                .isActive(true)
                .build();
        winnerMarket = marketRepository.save(winnerMarket);

        List<Selection> selections = new ArrayList<>();
        selections.add(Selection.builder().market(winnerMarket).name("1").odds(normalizeOdds(request.getHomeOdds(), "2.00")).isActive(true).build());
        if (isDrawPossible) {
            selections.add(Selection.builder().market(winnerMarket).name("X").odds(normalizeOdds(request.getDrawOdds(), "3.20")).isActive(true).build());
        }
        selections.add(Selection.builder().market(winnerMarket).name("2").odds(normalizeOdds(request.getAwayOdds(), "2.00")).isActive(true).build());
        selectionRepository.saveAll(selections);

        return event;
    }

    @Transactional(readOnly = true)
    public Event findEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Nie znaleziono wydarzenia o ID: " + id));
    }

    @Transactional(readOnly = true)
    public EventResponse getEventDetails(Long id) {
        Event event = findEventById(id);
        return mapToEventResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findAllEvents() {
        return eventRepository.findAllWithLeagueSport(PageRequest.of(0, DEFAULT_EVENT_LIMIT)).getContent().stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findAllEvents(Pageable pageable) {
        return eventRepository.findAllWithLeagueSport(pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional(readOnly = true)
    public long countActiveEvents() {
        return eventRepository.countByStatusIn(List.of(EventStatus.UPCOMING, EventStatus.LIVE));
    }

    private int safeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private BigDecimal normalizeOdds(BigDecimal odds, String defaultValue) {
        BigDecimal value = odds != null ? odds : new BigDecimal(defaultValue);
        if (value.compareTo(new BigDecimal("1.00")) < 0) {
            throw new IllegalArgumentException("Kurs nie może być mniejszy niż 1.00.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public EventResponse mapToEventResponse(Event event) {
        List<Market> markets = marketRepository.findByEventId(event.getId());
        Integer expectedDurationMinutes = event.getExpectedDurationMinutes() != null ? event.getExpectedDurationMinutes() : 90;

        List<MarketResponse> marketResponses = markets.stream().map(market -> {
            List<Selection> selections = selectionRepository.findByMarketId(market.getId());
            List<SelectionResponse> selectionResponses = selections.stream().map(sel -> 
                SelectionResponse.builder()
                        .id(sel.getId())
                        .name(sel.getName())
                        .odds(sel.getOdds())
                        .isWinner(sel.getIsWinner())
                        .isActive(sel.getIsActive())
                        .build()
            ).collect(Collectors.toList());

            return MarketResponse.builder()
                    .id(market.getId())
                    .name(market.getName())
                    .description(market.getDescription())
                    .isSettled(market.getIsSettled())
                    .selections(selectionResponses)
                    .build();
        }).collect(Collectors.toList());

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .teamA(event.getTeamA())
                .teamB(event.getTeamB())
                .startTime(event.getStartTime())
                .expectedDurationMinutes(expectedDurationMinutes)
                .endTime(event.getStartTime().plusMinutes(expectedDurationMinutes))
                .status(event.getStatus().name())
                .resultA(event.getResultA())
                .resultB(event.getResultB())
                .leagueName(event.getLeague().getName())
                .sportName(event.getLeague().getSport().getName())
                .markets(marketResponses)
                .build();
    }
}
