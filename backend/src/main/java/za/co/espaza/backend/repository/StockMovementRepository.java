package za.co.espaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.espaza.backend.Enum.MovementType;
import za.co.espaza.backend.entity.StockMovement;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    default List<StockMovement> findByProductIdAndCreatedAtBetween(String productId, LocalDateTime from, LocalDateTime to) {
        return null;
    }

    List<StockMovement> findByMovementTypeAndCreatedAtBetween(MovementType type, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT m
        FROM StockMovement m
        WHERE (:productId IS NULL
               OR m.productId = :productId)
          AND (:type IS NULL
               OR m.movementType = :type)
          AND (:fromDate IS NULL
               OR m.createdAt >= :fromDate)
          AND (:toDate IS NULL
               OR m.createdAt <= :toDate)
        ORDER BY m.createdAt DESC
        """)

    List<StockMovement> findWithFilters(
            @Param("productId") String productId,
            @Param("type") MovementType type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
