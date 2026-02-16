package com.product_service.component;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.product_service.domain.OrderCreatedEvent;
import com.product_service.dto.BulkReserveRequest;
import com.product_service.service.ProductService;

import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventListener {

    private final ObjectMapper objectMapper;
    private final ProductService productService;

    public OrderEventListener(ObjectMapper objectMapper,
                          ProductService productService) {
    this.objectMapper = objectMapper;
    this.productService = productService;
    }

    @KafkaListener(topics = "order-events")
    public void listen(String message, Acknowledgment ack) {

        try {

            OrderCreatedEvent event =
                objectMapper.readValue(message, OrderCreatedEvent.class);

            BulkReserveRequest request =
                new BulkReserveRequest(
                    event.orderId(),
                    event.items().stream()
                        .map(i -> new BulkReserveRequest.Item(
                            i.productId(),
                            i.quantity()))
                        .toList()
                );

            productService.reserveBulk(request);

            ack.acknowledge();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
