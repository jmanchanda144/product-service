package com.product_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.product_service.domain.UpdateProductStatusRequest;
import com.product_service.dto.BulkReserveRequest;
import com.product_service.dto.CreateProductRequest;
import com.product_service.dto.ProductResponse;
import com.product_service.service.ProductService;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @PostMapping("/bulk")
    public ResponseEntity<List<ProductResponse>> getBulk(
                                @RequestBody List<Long> ids) {

            return ResponseEntity.ok(
            productService.getProductsByIds(ids));
    }
    @PostMapping("/reserve-bulk")
    public ResponseEntity<?> reserveBulk(
        @RequestBody BulkReserveRequest request) {

    productService.reserveBulk(request);
    return ResponseEntity.ok("Bulk stock reserved");
}

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
    return ResponseEntity.ok(productService.getAllProducts());
    }

    @PostMapping
    public ResponseEntity<Long> createProduct(
            @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateProductStatusRequest request) {

        productService.updateStatus(id, request.status());
        return ResponseEntity.ok().build();
    }

   @PostMapping("/{id}/reserve")
public ResponseEntity<?> reserveStock(
        @PathVariable Long id,
        @RequestParam Long orderId,
        @RequestParam Integer qty) {

    productService.reserveStock(id, orderId, qty);
    return ResponseEntity.ok("Stock reserved");
}

@PostMapping("/release")
public ResponseEntity<?> releaseStock(
        @RequestParam Long orderId) {

    productService.releaseStock(orderId);
    return ResponseEntity.ok("Stock released");
}
@PostMapping("/confirm")
public ResponseEntity<?> confirmStock(@RequestParam Long orderId) {
    productService.confirmStock(orderId);
    return ResponseEntity.ok("Stock confirmed");
}


}