package za.co.espaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import za.co.espaza.backend.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository  extends JpaRepository<Product, String> {

    Optional<Product> findByBarcode(String barcode);
    List<Product> findByIsActiveTrue();
    List<Product> findByIsActiveTrueAndNameContainingIgnoreCase(String name);
    List<Product> findByIsActiveTrueAndCategory_CategoryId(String categoryId);
    List<Product> findByIsActiveTrueAndNameContainingIgnoreCaseAndCategory_CategoryId(String name, String categoryId);
    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
              AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(p.barcode, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            ORDER BY p.name ASC
            """)
    List<Product> searchActiveProducts(@Param("search") String search,
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
