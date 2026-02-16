package com.product_service.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.product_service.domain.InventoryOutboxEvent;
import com.product_service.domain.OutboxStatus;
import com.product_service.repository.InventoryOutboxRepository;

@Service
public class InventoryOutboxStatusService {

    private final InventoryOutboxRepository repo;

    public InventoryOutboxStatusService(InventoryOutboxRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long id) {

        InventoryOutboxEvent e = repo.findById(id).orElseThrow();

        e.setStatus(OutboxStatus.SENT);
        e.setProcessedAt(Instant.now());
        e.setProcessingStartedAt(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(Long id) {

        InventoryOutboxEvent e = repo.findById(id).orElseThrow();

        int retry = e.getRetryCount() == null ? 0 : e.getRetryCount();
        retry++;

        e.setRetryCount(retry);
        e.setProcessingStartedAt(null);

        if (retry >= 10)
            e.setStatus(OutboxStatus.DEAD);
        else
            e.setStatus(OutboxStatus.FAILED);
    }
}
