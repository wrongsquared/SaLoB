package com.salob.user_service.common.rabbitmq;

import com.salob.user_service.api._domain.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.scheduling.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ProcessedEventCleanupTask {

    private final ProcessedEventRepository processedEventRepo;

    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeOldEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = processedEventRepo.deleteByProcessedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} processed_events older than 7 days", deleted);
        }
    }
}
