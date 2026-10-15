package com.jobcrm.reminders.reminder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    /** The idempotent-upsert lookup used by the API in Part 3. */
    Optional<Reminder> findByUserIdAndClientJobId(UUID userId, UUID clientJobId);

    List<Reminder> findByUserIdOrderByRemindAtAsc(UUID userId);
}
