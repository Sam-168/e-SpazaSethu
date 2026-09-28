package za.co.espaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
              AND p.lowStockThreshold IS NOT NULL
              AND p.stockQuantity <= p.lowStockThreshold
            ORDER BY p.stockQuantity ASC, p.name ASC
            """)
    List<Product> findLowStockProducts();

}
