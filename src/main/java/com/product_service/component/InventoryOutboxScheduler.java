package com.product_service.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.product_service.service.InventoryOutboxService;

@Component
public class InventoryOutboxScheduler {

    private final InventoryOutboxService service;

    public InventoryOutboxScheduler(
            InventoryOutboxService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishEvents() {

        var events = service.lockBatch();
        events.forEach(service::publish);
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverStuck() {
        service.recoverStuck();
    }
    @Scheduled(fixedDelay = 3600000) // every 1 hour
    public void cleanupSent() {
        service.deleteOldSentEvents();
    }
}

