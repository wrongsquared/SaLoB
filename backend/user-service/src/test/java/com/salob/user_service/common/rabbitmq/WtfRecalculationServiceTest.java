package com.salob.user_service.common.rabbitmq;

import static org.junit.jupiter.api.Assertions.*;

import com.salob.user_service.api._domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WtfRecalculationServiceTest {

    private WtfRecalculationService service;

    @BeforeEach
    void setUp() {
        service = new WtfRecalculationService(null);
        // null UserRepository is fine — we're only testing computeWtfScore(),
        // which doesn't use the repository. The applyEvent() method does,
        // but we test it via the consumer integration test.
    }

    @Test
    void newUserGetsBaseline50() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .totalSubmissions(0)
            .upvotesReceived(0)
            .downvotesReceived(0)
            .anomaliesFlagged(0)
            .build();

        double score = service.computeWtfScore(user);
        assertEquals(50.0, score, 0.5);
        // 0.5 delta because tanh(0/10) = 0 exactly, small rounding
    }

    @Test
    void activeTrustedUserGetsHighScore() {
        // User with 1 year tenure, 20 submissions, 10 upvotes, 0 downvotes, active today
        User user = User.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now().minus(365, ChronoUnit.DAYS))
            .lastActivityAt(Instant.now())
            .totalSubmissions(20)
            .upvotesReceived(10)
            .downvotesReceived(0)
            .anomaliesFlagged(0)
            .build();

        double score = service.computeWtfScore(user);
        // tenure=100, vote=50+50*tanh(10/10)=50+50*0.76=88, flag=100, volume=100
        // raw = 0.15*100 + 0.40*88 + 0.30*100 + 0.15*100 = 15+35+30+15 = 95
        // multiplier = 1.0 (active today)
        assertEquals(95.0, score, 1.0);
    }

    @Test
    void inactiveUserGetsHalvedScore() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now().minus(365, ChronoUnit.DAYS))
            .lastActivityAt(Instant.now().minus(200, ChronoUnit.DAYS))
            .totalSubmissions(20)
            .upvotesReceived(10)
            .downvotesReceived(0)
            .anomaliesFlagged(0)
            .build();

        double score = service.computeWtfScore(user);
        // raw = 95, 200 days inactive
        // multiplier = 1.0 - 0.5 * min(200/180, 1) = 1.0 - 0.5 = 0.5
        // wtf = 95 * 0.5 = 47.5
        assertEquals(47.5, score, 1.0);
    }

    @Test
    void fullyFlaggedUserGetsZeroFlagScore() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .lastActivityAt(Instant.now())
            .totalSubmissions(5)
            .upvotesReceived(0)
            .downvotesReceived(0)
            .anomaliesFlagged(5)
            .build();

        double score = service.computeWtfScore(user);
        // tenure=0, vote=50, flag=100*(1-5/5)=0, volume=min(5/20,1)*100=25
        // raw = 0.15*0 + 0.40*50 + 0.30*0 + 0.15*25 = 20 + 3.75 = 23.75
        // active → multiplier = 1.0
        assertTrue(score < 30.0, "Fully flagged user should score below 30, got " + score);
    }

    @Test
    void heavilyDownvotedUserScoresLow() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .createdAt(Instant.now())
            .lastActivityAt(Instant.now())
            .totalSubmissions(10)
            .upvotesReceived(0)
            .downvotesReceived(20)
            .anomaliesFlagged(0)
            .build();

        double score = service.computeWtfScore(user);
        // vote = 50 + 50*tanh((0-20)/10) = 50 + 50*tanh(-2) = 50 + 50*(-0.96) = 2
        // flag = 100, volume = min(10/20,1)*100 = 50
        // raw = 0.15*0 + 0.40*2 + 0.30*100 + 0.15*50 = 0 + 0.8 + 30 + 7.5 = 38
        assertTrue(score < 50.0, "Heavily downvoted user should score below 50, got " + score);
    }
}
