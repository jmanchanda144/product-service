package com.product_service.service;

import java.time.Instant;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.product_service.domain.InventoryOutboxEvent;
import com.product_service.domain.OutboxStatus;
import com.product_service.repository.InventoryOutboxRepository;

@Service
public class InventoryOutboxService {

    private final InventoryOutboxRepository repo;
    private final KafkaTemplate<String,String> kafka;
    private final InventoryOutboxStatusService statusService;

    public InventoryOutboxService(
            InventoryOutboxRepository repo,
            KafkaTemplate<String,String> kafka,
        InventoryOutboxStatusService statusService) {
        this.repo = repo;
        this.kafka = kafka;
         this.statusService = statusService;
    }

    @Transactional
    public List<InventoryOutboxEvent> lockBatch() {

        List<InventoryOutboxEvent> events = repo.findBatchForUpdate();

        Instant now = Instant.now();

        for (var e : events) {
            e.setStatus(OutboxStatus.PROCESSING);
            e.setProcessingStartedAt(now);
        }

        return events;
    }

    public void publish(InventoryOutboxEvent event) {

        kafka.send(
                event.getTopic(),
                event.getAggregateId().toString(),
                event.getPayload()
        ).whenComplete((result, ex) -> {

            if (ex == null) {
                statusService.markSuccess(event.getId());
            } else {
               statusService.markFailure(event.getId());
            }
        });
    }

    @Transactional
    public void recoverStuck() {

        Instant threshold = Instant.now().minusSeconds(300);

        List<InventoryOutboxEvent> stuck =
            repo.findByStatusAndProcessingStartedAtBefore(
                OutboxStatus.PROCESSING,
                threshold);

        for (var e : stuck) {
            e.setStatus(OutboxStatus.FAILED);
            e.setProcessingStartedAt(null);   // IMPORTANT
        }
    }
    @Transactional
    public void deleteOldSentEvents() {

        Instant threshold = Instant.now().minusSeconds(86400); // 1 day

        repo.deleteByStatusAndProcessedAtBefore(
                OutboxStatus.SENT,
                threshold);
    }
}

