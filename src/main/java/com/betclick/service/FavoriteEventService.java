package com.betclick.service;

import com.betclick.dto.response.FavoriteEventResponse;
import com.betclick.model.Event;
import com.betclick.model.FavoriteEvent;
import com.betclick.model.User;
import com.betclick.repository.EventRepository;
import com.betclick.repository.FavoriteEventRepository;
import com.betclick.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteEventService {

    private final FavoriteEventRepository favoriteEventRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final DictionaryService dictionaryService;

    public FavoriteEventService(FavoriteEventRepository favoriteEventRepository,
                                UserRepository userRepository,
                                EventRepository eventRepository,
                                DictionaryService dictionaryService) {
        this.favoriteEventRepository = favoriteEventRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.dictionaryService = dictionaryService;
    }

    @Transactional
    public void addFavorite(String login, Long eventId) {
        User user = findUser(login);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono wydarzenia."));
        Long activeId = activeStatusId();

        FavoriteEvent favorite = favoriteEventRepository.findByUserIdAndEventId(user.getId(), eventId)
                .orElseGet(() -> FavoriteEvent.builder()
                        .user(user)
                        .event(event)
                        .createdAt(LocalDateTime.now())
                        .build());
        favorite.setStatusId(activeId);
        favoriteEventRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(String login, Long eventId) {
        User user = findUser(login);
        FavoriteEvent favorite = favoriteEventRepository.findByUserIdAndEventId(user.getId(), eventId)
                .orElseThrow(() -> new IllegalArgumentException("To wydarzenie nie jest na liscie ulubionych."));
        favorite.setStatusId(dictionaryService.id("favorite_statuses", "REMOVED"));
        favoriteEventRepository.save(favorite);
    }

    @Transactional(readOnly = true)
    public List<FavoriteEventResponse> getFavorites(String login) {
        User user = findUser(login);
        return favoriteEventRepository.findByUserIdAndStatusIdOrderByCreatedAtDesc(user.getId(), activeStatusId())
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> getFavoriteEventIds(String login) {
        User user = findUser(login);
        return favoriteEventRepository.findActiveEventIds(user.getId(), activeStatusId())
                .stream()
                .collect(Collectors.toSet());
    }

    private FavoriteEventResponse map(FavoriteEvent favorite) {
        Event event = favorite.getEvent();
        return FavoriteEventResponse.builder()
                .favoriteId(favorite.getId())
                .eventId(event.getId())
                .eventName(event.getName())
                .teamA(event.getTeamA())
                .teamB(event.getTeamB())
                .sportName(event.getLeague().getSport().getName())
                .leagueName(event.getLeague().getName())
                .status(event.getStatus().name())
                .startTime(event.getStartTime())
                .createdAt(favorite.getCreatedAt())
                .build();
    }

    private Long activeStatusId() {
        return dictionaryService.id("favorite_statuses", "ACTIVE");
    }

    private User findUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika: " + login));
    }
}
