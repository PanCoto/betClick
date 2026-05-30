package com.betclick.repository;

import com.betclick.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndStatusId(Long userId, Long statusId);
    boolean existsByUserIdAndTypeIdAndRelatedEntityTypeAndRelatedEntityId(
            Long userId,
            Long typeId,
            String relatedEntityType,
            Long relatedEntityId
    );
}
