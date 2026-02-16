package com.product_service.domain;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    FAILED,
    SENT,
    DEAD
}

