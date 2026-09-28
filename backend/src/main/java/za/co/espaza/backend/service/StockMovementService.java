package za.co.espaza.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.Enum.MovementType;
import za.co.espaza.backend.dto.request.AdjustmentRequest;
import za.co.espaza.backend.dto.response.StockMovementResponse;
import za.co.espaza.backend.dto.request.MovementFilterRequest;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.entity.StockMovement;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.repository.ProductRepository;
import za.co.espaza.backend.repository.StockMovementRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StockMovementService {
    @Autowired
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;

    public StockMovementService(StockMovementRepository movementRepository,ProductRepository productRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    public StockMovementResponse createManualAdjustment(
            AdjustmentRequest request,
            String userId
    ) {

        if (request.getNotes() == null
                || request.getNotes().trim().isEmpty()) {

            throw new BusinessRuleException(
                    "Adjustment notes are required"
            );
        }

        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Product not found: "
                                        + request.getProductId()
                        )
                );

        int currentStock = product.getStockQuantity() == null
                ? 0
                : product.getStockQuantity();

        int quantityChange = request.getQuantityChange();

        int newStock = currentStock + quantityChange;

        if (newStock < 0) {

            throw new BusinessRuleException(
                    "Adjustment cannot reduce stock below zero"
            );
        }

        product.setStockQuantity(newStock);

        productRepository.save(product);


        StockMovement movement = new StockMovement();

        movement.setProductId(product.getProductId());
        movement.setMovementType(MovementType.ADJUSTMENT);
        movement.setQuantityChange(quantityChange);
        movement.setUserId(userId);
        movement.setNotes(request.getNotes().trim());

        StockMovement savedMovement =
                movementRepository.save(movement);

        return toResponse(savedMovement);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements(
            MovementFilterRequest filters
    ) {

        validateDateRange(filters);

        return movementRepository
                .findWithFilters(
                        filters.getProductId(),
                        filters.getType(),
                        filters.getFrom(),
                        filters.getTo()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateDateRange(
            MovementFilterRequest filters
    ) {

        LocalDateTime from = filters.getFrom();
        LocalDateTime to = filters.getTo();

        if (from != null
                && to != null
                && from.isAfter(to)) {

            throw new BusinessRuleException(
                    "'from' date cannot be after 'to' date"
            );
        }
    }

    private StockMovementResponse toResponse(
            StockMovement movement
    ) {

        return new StockMovementResponse(
                movement.getMovementId(),
                movement.getProductId(),
                movement.getMovementType(),
                movement.getQuantityChange(),
                movement.getReferenceId(),
                movement.getUserId(),
                movement.getNotes(),
                movement.getCreatedAt()
        );
    }
}
