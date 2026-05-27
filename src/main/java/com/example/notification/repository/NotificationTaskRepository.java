package com.example.notification.repository;

import com.example.notification.domain.NotificationStatus;
import com.example.notification.domain.NotificationTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task
            from NotificationTask task
            where task.status = :status
              and task.nextRetryAt <= :now
            order by task.nextRetryAt asc
            """)
    List<NotificationTask> findDueTasksForUpdate(NotificationStatus status, Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task
            from NotificationTask task
            where task.status = :status
              and task.updatedAt < :deadline
            """)
    List<NotificationTask> findStaleProcessingTasksForUpdate(NotificationStatus status, Instant deadline);
}
