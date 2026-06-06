package com.betclick.service;

import com.betclick.dto.request.SettleEventRequest;
import com.betclick.model.Event;
import com.betclick.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.auto-settle.enabled", havingValue = "true", matchIfMissing = true)
public class EventAutoSettler {

    private static final Logger log = LoggerFactory.getLogger(EventAutoSettler.class);

    private final EventRepository eventRepository;
    private final ResultGenerator resultGenerator;
    private final WinningSelectionResolver winningSelectionResolver;
    private final AdminService adminService;

    public EventAutoSettler(EventRepository eventRepository,
                            ResultGenerator resultGenerator,
                            WinningSelectionResolver winningSelectionResolver,
                            AdminService adminService) {
        this.eventRepository = eventRepository;
        this.resultGenerator = resultGenerator;
        this.winningSelectionResolver = winningSelectionResolver;
        this.adminService = adminService;
    }

    @Scheduled(fixedRateString = "${app.auto-settle.interval-ms:60000}")
    public void autoSettleEvents() {
        log.debug("Auto-settler tick");
        try {
            List<Event> eventsToSettle = eventRepository.findEventsToAutoSettle();
            if (eventsToSettle.isEmpty()) {
                log.debug("Found 0 events to settle");
                return;
            }

            List<Long> eventIds = eventsToSettle.stream()
                    .map(Event::getId)
                    .toList();
            eventsToSettle = eventRepository.findAllByIdWithLeagueSport(eventIds);

            log.info("Found {} events to settle", eventsToSettle.size());
            for (Event event : eventsToSettle) {
                try {
                    String sportName = "Unknown";
                    if (event.getLeague() != null && event.getLeague().getSport() != null) {
                        sportName = event.getLeague().getSport().getName();
                    }

                    log.info("Settling event id={}, name='{}', sport='{}', startTime='{}', expectedDuration={}m",
                            event.getId(), event.getName(), sportName, event.getStartTime(), event.getExpectedDurationMinutes());

                    ResultGenerator.SimulatedResult result = resultGenerator.generateResult(event);
                    log.info("Simulated result for event ID={}: {}-{}", event.getId(), result.getScoreA(), result.getScoreB());

                    List<Long> winningSelectionIds = winningSelectionResolver.resolveWinningSelections(
                            event.getId(), result.getScoreA(), result.getScoreB());
                    log.info("Resolved winning selections for event ID={}: {}", event.getId(), winningSelectionIds);

                    SettleEventRequest request = new SettleEventRequest();
                    request.setResultA(result.getScoreA());
                    request.setResultB(result.getScoreB());
                    request.setWinningSelectionIds(winningSelectionIds);

                    adminService.settleEvent(event.getId(), request);
                    log.info("Event id={} settled successfully", event.getId());
                } catch (Exception e) {
                    log.error("Event id={} settlement failed", event.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error occurred during auto-settlement job", e);
        }
    }
}
