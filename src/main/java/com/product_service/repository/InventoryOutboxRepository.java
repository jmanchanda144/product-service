package com.product_service.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.product_service.domain.InventoryOutboxEvent;
import com.product_service.domain.OutboxStatus;

public interface InventoryOutboxRepository
        extends JpaRepository<InventoryOutboxEvent, Long> {
    @Query(value = """
    SELECT *
    FROM inventory_outbox_events
    WHERE status IN ('PENDING','FAILED')
    ORDER BY created_at
    LIMIT 100
    FOR UPDATE SKIP LOCKED
""", nativeQuery = true)
List<InventoryOutboxEvent> findBatchForUpdate();

    List<InventoryOutboxEvent>
        findByStatusAndProcessingStartedAtBefore(
            OutboxStatus status,
            Instant time);
    void deleteByStatusAndProcessedAtBefore(
        OutboxStatus status,
        Instant time);
}

