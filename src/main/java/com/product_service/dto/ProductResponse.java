package com.product_service.dto;

import java.math.BigDecimal;

import com.product_service.domain.ProductStatus;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        ProductStatus status,
        Integer availableQuantity
) {}
