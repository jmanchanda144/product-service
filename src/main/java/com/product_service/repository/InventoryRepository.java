package com.product_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.product_service.domain.Inventory;

public interface  InventoryRepository extends JpaRepository<Inventory, Long>{
List<Inventory> findAllByProductIdIn(List<Long> productIds);

@Modifying
@Query("""
    UPDATE Inventory i
    SET i.availableQuantity = i.availableQuantity - :qty,
        i.updatedAt = CURRENT_TIMESTAMP
    WHERE i.productId = :productId
    AND i.availableQuantity >= :qty
""")
int reserveStockIfActive(@Param("productId") Long productId,
                         @Param("qty") Integer qty);



    @Modifying
    @Query("""
        UPDATE Inventory i
        SET i.availableQuantity = i.availableQuantity + :qty,
            i.updatedAt = CURRENT_TIMESTAMP
        WHERE i.productId = :productId
    """)
    int releaseStock(@Param("productId") Long productId,
                     @Param("qty") Integer qty);
}
