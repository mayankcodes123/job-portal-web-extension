package com.jobcrm.reminders.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Expects an already-normalized address (see {@link User#normalizeEmail(String)}). */
    Optional<User> findByEmail(String email);
}
