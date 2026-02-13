package com.product_service.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.product_service.domain.Inventory;
import com.product_service.domain.InventoryReservation;
import com.product_service.domain.Product;
import com.product_service.domain.ProductStatus;
import com.product_service.domain.ReservationStatus;
import com.product_service.dto.BulkReserveRequest;
import com.product_service.dto.CreateProductRequest;
import com.product_service.dto.ProductResponse;
import com.product_service.exceptions.OutOfStockException;
import com.product_service.id.SnowflakeIdGenerator;
import com.product_service.repository.InventoryRepository;
import com.product_service.repository.InventoryReservationRepository;
import com.product_service.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final InventoryReservationRepository inventoryReservationRepository;

    public ProductService(ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          SnowflakeIdGenerator idGenerator,
                        InventoryReservationRepository inventoryReservationRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.idGenerator = idGenerator;
        this.inventoryReservationRepository=inventoryReservationRepository;
    }

    @Transactional
    public Long createProduct(CreateProductRequest request) {

        long productId = idGenerator.nextId();
        Instant now = Instant.now();

        Product product = new Product();
        product.setId(productId);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setAvailableQuantity(request.initialQuantity());
        inventory.setUpdatedAt(now);

        inventoryRepository.save(inventory);

        return productId;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                inventory.getAvailableQuantity()
        );
    }

    @Transactional
    public void updateStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStatus(status);
        product.setUpdatedAt(Instant.now());
    }

@Transactional
public void reserveStock(Long productId, Long orderId, Integer qty) {

    
    if (orderId == null) {
    throw new IllegalArgumentException("Invalid orderId");
    }

    if (qty == null || qty <= 0) {
        throw new IllegalArgumentException("Invalid quantity");
    }
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

    if (product.getStatus() != ProductStatus.ACTIVE) {
        throw new IllegalStateException("Product is not active");
    }

    InventoryReservation existing =
        inventoryReservationRepository
            .findByOrderIdAndProductId(orderId, productId)
            .orElse(null);

    if (existing != null) {
        if (existing.getStatus() == ReservationStatus.PENDING) {
            return; // idempotent retry
        }
        throw new IllegalStateException("Reservation already finalized");
    }

    int updated = inventoryRepository.reserveStockIfActive(productId, qty);

    if (updated == 0) {
        throw new OutOfStockException("Not enough stock or inactive");
    }

    InventoryReservation reservation = new InventoryReservation();
    reservation.setReservationId(idGenerator.nextId());
    reservation.setProductId(productId);
    reservation.setOrderId(orderId);
    reservation.setQuantity(qty);
    Instant now = Instant.now();
    reservation.setCreatedAt(now);
    reservation.setExpiresAt(now.plusSeconds(600));
    reservation.setStatus(ReservationStatus.PENDING);

    inventoryReservationRepository.save(reservation);
}

@Transactional
public void confirmStock(Long orderId) {

    if (orderId == null)
        throw new IllegalArgumentException("Invalid orderId");

    inventoryReservationRepository.confirmAll(
            orderId,
            Instant.now());
}



@Transactional
public void releaseStock(Long orderId) {

    if (orderId == null)
        throw new IllegalArgumentException("Invalid orderId");

    List<InventoryReservation> reservations =
            inventoryReservationRepository
                    .findAllByOrderIdAndStatus(
                            orderId,
                            ReservationStatus.PENDING);

    if (reservations.isEmpty())
        return;

    int updated =
            inventoryReservationRepository.releaseAll(orderId);

    if (updated > 0) {
        for (var r : reservations) {
            inventoryRepository.releaseStock(
                    r.getProductId(),
                    r.getQuantity());
        }
    }
}




@Scheduled(fixedDelay = 10000)
@Transactional
public void cleanupExpiredReservations() {

    Instant now = Instant.now();

    List<InventoryReservation> expired =
            inventoryReservationRepository
                    .findExpiredForUpdate(
                            now,
                            PageRequest.of(0, 500));

    for (InventoryReservation r : expired) {
        r.setStatus(ReservationStatus.RELEASED);

        inventoryRepository.releaseStock(
                r.getProductId(),
                r.getQuantity());
    }
}

@Transactional(readOnly = true)
public List<ProductResponse> getAllProducts() {

    List<Product> products = productRepository.findAll();

    List<Long> ids = products.stream()
            .map(Product::getId)
            .toList();

    List<Inventory> inventories =
            inventoryRepository.findAllByProductIdIn(ids);

    Map<Long, Integer> inventoryMap =
            inventories.stream()
                    .collect(Collectors.toMap(
                            Inventory::getProductId,
                            Inventory::getAvailableQuantity
                    ));

    return products.stream()
            .map(product -> new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStatus(),
                    inventoryMap.getOrDefault(product.getId(), 0)
            ))
            .toList();
    }
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByIds(List<Long> ids) {

        List<Product> products = productRepository.findAllById(ids);

        List<Inventory> inventories =
            inventoryRepository.findAllByProductIdIn(ids);

        Map<Long, Integer> inventoryMap =
            inventories.stream()
                    .collect(Collectors.toMap(
                            Inventory::getProductId,
                            Inventory::getAvailableQuantity
                    ));

        return products.stream()
            .map(p -> new ProductResponse(
                    p.getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getPrice(),
                    p.getStatus(),
                    inventoryMap.getOrDefault(p.getId(), 0)
            ))
            .toList();
    }
@Transactional
public void reserveBulk(BulkReserveRequest request) {

    Long orderId = request.orderId();

    if (orderId == null)
        throw new IllegalArgumentException("Invalid orderId");

    if (request.items() == null || request.items().isEmpty())
        throw new IllegalArgumentException("No items");

    Instant now = Instant.now();

    // fetch existing reservations once
    List<InventoryReservation> existing =
            inventoryReservationRepository.findAllByOrderId(orderId);

    Map<Long, InventoryReservation> existingMap =
            existing.stream()
                    .collect(Collectors.toMap(
                            InventoryReservation::getProductId,
                            r -> r));

    // fetch all products once
    List<Long> productIds =
            request.items()
                    .stream()
                    .map(BulkReserveRequest.Item::productId)
                    .toList();

    List<Product> products =
            productRepository.findAllById(productIds);

    Map<Long, Product> productMap =
            products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

    for (var item : request.items()) {

        if (item.quantity() == null || item.quantity() <= 0)
            throw new IllegalArgumentException("Invalid quantity");

        Product product = productMap.get(item.productId());

        if (product == null)
            throw new RuntimeException("Product not found");

        if (product.getStatus() != ProductStatus.ACTIVE)
            throw new IllegalStateException("Product inactive");

        InventoryReservation old =
                existingMap.get(item.productId());

        if (old != null) {
            if (old.getStatus() == ReservationStatus.PENDING)
                continue;
            else
                throw new IllegalStateException("Already finalized");
        }

        int updated =
                inventoryRepository.reserveStockIfActive(
                        item.productId(),
                        item.quantity());

        if (updated == 0)
            throw new OutOfStockException(
                    "Out of stock for product " + item.productId());

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(idGenerator.nextId());
        reservation.setProductId(item.productId());
        reservation.setOrderId(orderId);
        reservation.setQuantity(item.quantity());
        reservation.setCreatedAt(now);
        reservation.setExpiresAt(now.plusSeconds(600));
        reservation.setStatus(ReservationStatus.PENDING);

        inventoryReservationRepository.save(reservation);
    }
}

}