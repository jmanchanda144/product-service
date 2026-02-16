package com.product_service.domain;

public record InventoryReservedEvent(
        String eventType,
        Long orderId
) {}
