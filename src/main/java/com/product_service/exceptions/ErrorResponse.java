package com.product_service.exceptions;

public record ErrorResponse(
    String error,
    String message
) {}

