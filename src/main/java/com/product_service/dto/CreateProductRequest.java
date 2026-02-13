package com.product_service.dto;

import java.math.BigDecimal;

public record CreateProductRequest(
    String name,
    String description,
    BigDecimal price,
    Integer initialQuantity
)
{}
