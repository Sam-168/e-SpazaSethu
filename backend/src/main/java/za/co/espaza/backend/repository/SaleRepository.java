package za.co.espaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import za.co.espaza.backend.entity.Sale;
import za.co.espaza.backend.enums.SaleStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sale s WHERE s.saleId = :id")
    Optional<Sale> findByIdForUpdate(UUID id);

    // Sales for a given user within a date range (sales history / cashier report)
    List<Sale> findByUserIdAndSaleDateTimeBetween(UUID userId, LocalDateTime from, LocalDateTime to);

    // Sales within a date range filtered by status (dashboard / reports)
    List<Sale> findBySaleDateTimeBetweenAndStatus(LocalDateTime from, LocalDateTime to, SaleStatus status);

    // Sales within a date range (sales history)
    List<Sale> findBySaleDateTimeBetween(LocalDateTime from, LocalDateTime to);
}
