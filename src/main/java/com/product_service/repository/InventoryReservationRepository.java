package com.product_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.product_service.domain.InventoryReservation;
import com.product_service.domain.ReservationStatus;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation>
    findByOrderIdAndProductId(Long orderId, Long productId);

    List<InventoryReservation>
    findAllByOrderId(Long orderId);

    List<InventoryReservation>
    findAllByOrderIdAndStatus(Long orderId, ReservationStatus status);

    @Query(
        value = """
            SELECT *
            FROM inventory_reservations r
            WHERE r.status = 'PENDING'
            AND r.expires_at <= :now
            ORDER BY r.expires_at ASC
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    List<InventoryReservation> findExpiredForUpdate(
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying
    @Query("""
        UPDATE InventoryReservation r
        SET r.status = 'CONFIRMED'
        WHERE r.orderId = :orderId
        AND r.status = 'PENDING'
        AND r.expiresAt > :now
    """)
    int confirmAll(@Param("orderId") Long orderId,
                   @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE InventoryReservation r
        SET r.status = 'RELEASED'
        WHERE r.orderId = :orderId
        AND r.status = 'PENDING'
    """)
    int releaseAll(@Param("orderId") Long orderId);

    @Modifying
    @Query("""
        UPDATE InventoryReservation r
        SET r.status = 'RELEASED'
        WHERE r.reservationId = :id
        AND r.status = 'PENDING'
    """)
    int markReleased(@Param("id") Long id);
}
