package com.product_service.domain;

public record OrderCreatedEvent(
        Long orderId,
        Long userId
) {}

