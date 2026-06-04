package com.betclick.service;

import com.betclick.dto.response.NotificationResponse;
import com.betclick.model.Notification;
import com.betclick.model.User;
import com.betclick.repository.NotificationRepository;
import com.betclick.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final int DEFAULT_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DictionaryService dictionaryService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               DictionaryService dictionaryService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.dictionaryService = dictionaryService;
    }

    @Transactional
    public void notify(User user, String typeCode, String title, String message,
                       String relatedEntityType, Long relatedEntityId) {
        Long typeId = dictionaryService.id("notification_types", typeCode);
        if (relatedEntityType != null && relatedEntityId != null
                && notificationRepository.existsByUserIdAndTypeIdAndRelatedEntityTypeAndRelatedEntityId(
                user.getId(), typeId, relatedEntityType, relatedEntityId)) {
            return;
        }

        notificationRepository.save(Notification.builder()
                .user(user)
                .typeId(typeId)
                .statusId(dictionaryService.id("notification_statuses", "UNREAD"))
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String login) {
        User user = findUser(login);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, DEFAULT_LIMIT))
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(String login) {
        User user = findUser(login);
        return countUnread(user.getId());
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndStatusId(
                userId,
                dictionaryService.id("notification_statuses", "UNREAD")
        );
    }

    @Transactional
    public void markAsRead(String login, Long notificationId) {
        User user = findUser(login);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono powiadomienia."));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Brak uprawnien do tego powiadomienia.");
        }
        if (notification.getReadAt() == null) {
            notification.setStatusId(dictionaryService.id("notification_statuses", "READ"));
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    private User findUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika: " + login));
    }

    private NotificationResponse map(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .typeCode(dictionaryService.code("notification_types", notification.getTypeId()))
                .statusCode(dictionaryService.code("notification_statuses", notification.getStatusId()))
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
