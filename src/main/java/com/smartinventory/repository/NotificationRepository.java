package com.smartinventory.repository;

import com.smartinventory.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE " +
            "(:userId IS NULL OR n.recipient.id = :userId) AND " +
            "(:role IS NULL OR n.recipientRole = :role) " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findVisible(@Param("userId") Long userId,
                                   @Param("role") String role,
                                   Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
            "n.readAt IS NULL AND " +
            "((:userId IS NOT NULL AND n.recipient.id = :userId) OR (:role IS NOT NULL AND n.recipientRole = :role))")
    long countUnread(@Param("userId") Long userId, @Param("role") String role);

    List<Notification> findTop10ByOrderByCreatedAtDesc();
}
