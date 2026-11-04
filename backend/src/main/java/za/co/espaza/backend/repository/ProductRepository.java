package za.co.espaza.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.espaza.backend.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByBarcode(String barcode);
    List<Product> findByIsActiveTrue();
    List<Product> findByIsActiveTrueAndCategory_CategoryId(String categoryId);

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN p.category c
            WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR REPLACE(REPLACE(COALESCE(p.barcode, ''), ' ', ''), '-', '')
                      LIKE CONCAT('%', :barcodeSearch, '%'))
              AND (:categoryId IS NULL OR c.categoryId = :categoryId)
            ORDER BY p.isActive DESC, p.name ASC
            """)
    List<Product> searchAllProducts(@Param("search") String search,
                                    @Param("barcodeSearch") String barcodeSearch,
                                    @Param("categoryId") String categoryId);

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN p.category c
            WHERE p.isActive = true
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR REPLACE(REPLACE(COALESCE(p.barcode, ''), ' ', ''), '-', '')
                      LIKE CONCAT('%', :barcodeSearch, '%'))
              AND (:categoryId IS NULL OR c.categoryId = :categoryId)
            ORDER BY p.name ASC
            """)
    List<Product> searchActiveProducts(@Param("search") String search,
                                       @Param("barcodeSearch") String barcodeSearch,
                                       @Param("categoryId") String categoryId);

    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
              AND p.lowStockThreshold IS NOT NULL
              AND p.stockQuantity <= p.lowStockThreshold
            ORDER BY p.stockQuantity ASC, p.name ASC
            """)
    List<Product> findLowStockProducts();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :id")
    Optional<Product> findByIdForUpdate(String id);
}
