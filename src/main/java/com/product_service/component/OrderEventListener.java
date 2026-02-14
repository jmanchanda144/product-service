package com.product_service.component;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.product_service.domain.OrderCreatedEvent;

import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventListener {

    private final ObjectMapper objectMapper;

    public OrderEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events")
    public void listen(String message) throws Exception {

        OrderCreatedEvent event =
                objectMapper.readValue(message, OrderCreatedEvent.class);

        System.out.println("🔥 Received order: " + event.orderId());
    }
}
