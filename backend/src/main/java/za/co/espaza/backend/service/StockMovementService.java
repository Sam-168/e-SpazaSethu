package za.co.espaza.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.Enum.MovementType;
import za.co.espaza.backend.dto.request.AdjustmentRequest;
import za.co.espaza.backend.dto.request.MovementFilterRequest;
import za.co.espaza.backend.dto.response.StockMovementResponse;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.entity.StockMovement;
import za.co.espaza.backend.entity.User;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.repository.ProductRepository;
import za.co.espaza.backend.repository.StockMovementRepository;
import za.co.espaza.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockMovementService {
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public StockMovementService(StockMovementRepository movementRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public StockMovementResponse createManualAdjustment(AdjustmentRequest request, String userId) {
        int quantityChange = request.getQuantityChange();
        if (quantityChange == 0) {
            throw new BusinessRuleException("Quantity change cannot be zero");
        }

        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.getProductId()));
        int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        int newStock = currentStock + quantityChange;
        if (newStock < 0) {
            throw new BusinessRuleException("Adjustment cannot reduce stock below zero");
        }

        product.setStockQuantity(newStock);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProductId(product.getProductId());
        movement.setMovementType(MovementType.ADJUSTMENT);
        movement.setQuantityChange(quantityChange);
        movement.setCreatedBy(userId);
        movement.setNotes(request.getNotes().trim());
        StockMovement saved = movementRepository.save(movement);
        String username = userRepository.findById(userId).map(User::getUsername).orElse("Unknown user");
        return toResponse(saved, product.getName(), username);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovements(MovementFilterRequest filters) {
        validateDateRange(filters);
        List<StockMovement> movements = movementRepository.findWithFilters(
                blankToNull(filters.getProductId()), filters.getType(), filters.getFrom(), filters.getTo());
        Map<String, String> productNames = productRepository.findAllById(
                        movements.stream().map(StockMovement::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getProductId, Product::getName));
        Map<String, String> usernames = userRepository.findAllById(
                        movements.stream().map(StockMovement::getCreatedBy).distinct().toList())
                .stream().collect(Collectors.toMap(User::getUserId, User::getUsername));
        return movements.stream().map(movement -> toResponse(
                movement,
                productNames.getOrDefault(movement.getProductId(), "Unknown product"),
                usernames.getOrDefault(movement.getCreatedBy(), "Unknown user")))
                .toList();
    }

    private void validateDateRange(MovementFilterRequest filters) {
        LocalDateTime from = filters.getFrom();
        LocalDateTime to = filters.getTo();
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("'from' date cannot be after 'to' date");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private StockMovementResponse toResponse(StockMovement movement, String productName, String username) {
        return new StockMovementResponse(
                movement.getMovementId(), movement.getProductId(), productName,
                movement.getMovementType(), movement.getQuantityChange(), movement.getReferenceId(),
                movement.getCreatedBy(), username, movement.getNotes(), movement.getCreatedAt());
    }
}
