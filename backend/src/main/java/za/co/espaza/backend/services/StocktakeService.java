package za.co.espaza.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.dto.request.StocktakeCountRequest;
import za.co.espaza.backend.dto.response.StocktakeResponse;
import za.co.espaza.backend.entity.Stocktake;
import za.co.espaza.backend.entity.StocktakeItem;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.entity.StockMovement;
import za.co.espaza.backend.Enum.MovementType;
import za.co.espaza.backend.enums.StocktakeStatus;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.exception.EntityNotFoundException;
import za.co.espaza.backend.repository.StocktakeRepository;
import za.co.espaza.backend.repository.ProductRepository;
import za.co.espaza.backend.repository.StockMovementRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Coordinates the stocktake snapshot, counts and resulting stock corrections. */
@Service
public class StocktakeService {

    private final StocktakeRepository stocktakeRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    public StocktakeService(StocktakeRepository stocktakeRepository,
                            ProductRepository productRepository,
                            StockMovementRepository movementRepository) {
        this.stocktakeRepository = stocktakeRepository;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    /** Starts a stocktake by snapshotting every active product's current quantity. */
    @Transactional
    public StocktakeResponse startStocktake(UUID userId) {
        stocktakeRepository.findFirstByStatusOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    throw new BusinessRuleException("A stocktake is already in progress");
                });

        List<Product> activeProducts = productRepository.findByIsActiveTrue();
        if (activeProducts.isEmpty()) {
            throw new BusinessRuleException("Add at least one active product before starting a stocktake");
        }

        Stocktake stocktake = new Stocktake(userId, null);
        activeProducts.forEach(product ->
                stocktake.addItem(new StocktakeItem(product.getProductId(), product.getStockQuantity())));
        return toResponse(stocktakeRepository.save(stocktake));
    }

    /** Records physical counts and derives each item's discrepancy. */
    @Transactional
    public StocktakeResponse submitCounts(UUID stocktakeId, List<StocktakeCountRequest> counts) {
        Stocktake stocktake = getInProgressStocktake(stocktakeId);

        if (counts == null) {
            throw new BusinessRuleException("Counts are required");
        }

        Map<UUID, StocktakeItem> itemsById = stocktake.getItems().stream()
                .collect(Collectors.toMap(StocktakeItem::getStocktakeItemId, item -> item));

        for (StocktakeCountRequest count : counts) {
            if (count == null) {
                throw new BusinessRuleException("Each count must contain a stocktake item and quantity");
            }
            if (count.stocktakeItemId() == null) {
                throw new BusinessRuleException("Each count must reference a stocktake item");
            }
            if (count.countedQuantity() != null && count.countedQuantity() < 0) {
                throw new BusinessRuleException("Counted quantities cannot be negative");
            }
            StocktakeItem item = itemsById.get(count.stocktakeItemId());
            if (item == null) {
                throw new EntityNotFoundException("Stocktake item not found: " + count.stocktakeItemId());
            }
            // A null countedQuantity means the item was skipped; discrepancy is cleared.
            item.setCountedQuantity(count.countedQuantity());
        }

        stocktakeRepository.save(stocktake);
        return toResponse(stocktake);
    }

    /** Includes a product registered after the stocktake snapshot was started. */
    @Transactional
    public StocktakeResponse addProduct(UUID stocktakeId, String productId) {
        Stocktake stocktake = getInProgressStocktake(stocktakeId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        if (!product.getIsActive()) {
            throw new BusinessRuleException("Inactive products cannot be counted");
        }
        if (stocktake.getItems().stream().anyMatch(item -> item.getProductId().equals(productId))) {
            return toResponse(stocktake);
        }
        stocktake.addItem(new StocktakeItem(productId, product.getStockQuantity()));
        return toResponse(stocktakeRepository.save(stocktake));
    }

    /** Applies each counted discrepancy, records the audit trail and completes the stocktake. */
    @Transactional
    public StocktakeResponse applyAdjustments(UUID stocktakeId, UUID userId) {
        Stocktake stocktake = getInProgressStocktake(stocktakeId);

        long uncounted = stocktake.getItems().stream()
                .filter(item -> item.getCountedQuantity() == null)
                .count();
        if (uncounted > 0) {
            throw new BusinessRuleException("Count every product before applying the stocktake");
        }

        for (StocktakeItem item : stocktake.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + item.getProductId()));
            int quantityChange = item.getCountedQuantity() - product.getStockQuantity();
            if (quantityChange != 0) {
                product.setStockQuantity(item.getCountedQuantity());
                productRepository.save(product);

                StockMovement movement = new StockMovement();
                movement.setProductId(product.getProductId());
                movement.setCreatedBy(userId.toString());
                movement.setQuantityChange(quantityChange);
                movement.setMovementType(MovementType.STOCKTAKE_CORRECTION);
                movement.setReferenceId(stocktakeId.toString());
                movement.setNotes("Stocktake correction");
                movementRepository.save(movement);
            }
            item.applyAdjustment();
        }

        stocktake.complete();
        return toResponse(stocktakeRepository.save(stocktake));
    }

    @Transactional(readOnly = true)
    public List<StocktakeResponse> getStocktakes() {
        return stocktakeRepository.findAllByOrderByStartedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StocktakeResponse cancelStocktake(UUID stocktakeId) {
        Stocktake stocktake = getInProgressStocktake(stocktakeId);
        stocktake.cancel();
        return toResponse(stocktakeRepository.save(stocktake));
    }

    @Transactional(readOnly = true)
    public StocktakeResponse getStocktakeById(UUID id) {
        Stocktake stocktake = stocktakeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stocktake not found: " + id));
        return toResponse(stocktake);
    }

    private Stocktake getInProgressStocktake(UUID stocktakeId) {
        Stocktake stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new EntityNotFoundException("Stocktake not found: " + stocktakeId));

        if (stocktake.getStatus() != StocktakeStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Stocktake is not in progress");
        }
        return stocktake;
    }

    private StocktakeResponse toResponse(Stocktake stocktake) {
        List<Product> products = productRepository.findAllById(
                stocktake.getItems().stream().map(StocktakeItem::getProductId).toList());
        Map<String, String> productNames = products.stream()
                .collect(Collectors.toMap(Product::getProductId, Product::getName));
        Map<String, String> productBarcodes = products.stream()
                .filter(product -> product.getBarcode() != null)
                .collect(Collectors.toMap(Product::getProductId, Product::getBarcode));
        return StocktakeResponse.from(stocktake, productNames, productBarcodes);
    }
}
