package za.co.espaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.espaza.backend.entity.Stocktake;
import za.co.espaza.backend.enums.StocktakeStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StocktakeRepository extends JpaRepository<Stocktake, UUID> {

    // Shop-wide history, newest first. A stocktake is an inventory-wide operation.
    List<Stocktake> findAllByOrderByStartedAtDesc();

    // Only one shop-wide stocktake may be active at a time.
    Optional<Stocktake> findFirstByStatusOrderByStartedAtDesc(StocktakeStatus status);
}
