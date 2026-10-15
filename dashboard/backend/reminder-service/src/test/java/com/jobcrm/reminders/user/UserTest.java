package com.jobcrm.reminders.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void emailIsTrimmedAndLowerCased() {
        assertThat(new User("  Candidate@Example.COM ").getEmail()).isEqualTo("candidate@example.com");
    }

    @Test
    void rejectsMissingOrBlankEmail() {
        assertThatThrownBy(() -> new User(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOverlongEmail() {
        String tooLong = "a".repeat(User.EMAIL_MAX_LENGTH) + "@example.com";
        assertThatThrownBy(() -> new User(tooLong)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void firstVerificationTimeIsKept() {
        User user = new User("candidate@example.com");
        Instant first = Instant.parse("2026-10-15T00:00:00Z");

        user.markVerified(first);
        user.markVerified(Instant.parse("2026-11-01T00:00:00Z"));

        assertThat(user.getVerifiedAt()).isEqualTo(first);
    }
}
