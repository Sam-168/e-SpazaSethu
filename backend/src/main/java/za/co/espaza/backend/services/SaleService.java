package za.co.espaza.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.dto.response.SaleResponse;
import za.co.espaza.backend.dto.request.CreateSaleRequest;
import za.co.espaza.backend.entity.Sale;
import za.co.espaza.backend.entity.SaleItem;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.entity.StockMovement;
import za.co.espaza.backend.entity.User;
import za.co.espaza.backend.Enum.MovementType;
import za.co.espaza.backend.enums.SaleStatus;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.exception.EntityNotFoundException;
import za.co.espaza.backend.repository.SaleRepository;
import za.co.espaza.backend.repository.ProductRepository;
import za.co.espaza.backend.repository.StockMovementRepository;
import za.co.espaza.backend.repository.UserRepository;
import za.co.espaza.backend.service.SaleCostSnapshotService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/** Coordinates checkout, sales history and cancellation. */
@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final SaleCostSnapshotService costSnapshots;

    public SaleService(SaleRepository saleRepository,
                       ProductRepository productRepository,
                       StockMovementRepository movementRepository,
                       UserRepository userRepository,
                       SaleCostSnapshotService costSnapshots) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
        this.costSnapshots = costSnapshots;
    }

    @Transactional
    public SaleResponse createSale(CreateSaleRequest request, UUID userId) {
        UUID saleId = request.clientRequestId() == null ? UUID.randomUUID() : request.clientRequestId();
        var existing = saleRepository.findById(saleId);
        if (existing.isPresent()) {
            if (!existing.get().getUserId().equals(userId)) {
                throw new BusinessRuleException("This checkout request id has already been used");
            }
            return toResponse(existing.get());
        }
        Map<String, Integer> quantities = new LinkedHashMap<>();
        request.items().forEach(item -> quantities.merge(item.productId(), item.quantity(), Integer::sum));

        Sale sale = new Sale(saleId, userId, request.paymentMethod(), SaleStatus.COMPLETED, request.notes());
        Map<String, Product> products = new LinkedHashMap<>();

        quantities.forEach((productId, quantity) -> {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw new BusinessRuleException("Product is inactive: " + product.getName());
            }
            product.deductStock(quantity);
            products.put(productId, product);

            SaleItem item = new SaleItem(productId, quantity, product.getSellingPrice());
            item.setSaleItemId(UUID.randomUUID());
            sale.addItem(item);
        });

        sale.calculateTotal();
        Sale saved = saleRepository.saveAndFlush(sale);
        productRepository.saveAll(products.values());

        for (SaleItem item : saved.getItems()) {
            costSnapshots.record(item, products.get(item.getProductId()).getCostPrice());
        }

        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            StockMovement movement = new StockMovement();
            movement.setProductId(entry.getKey());
            movement.setCreatedBy(userId.toString());
            movement.setQuantityChange(-entry.getValue());
            movement.setMovementType(MovementType.SALE);
            movement.setReferenceId(saved.getSaleId().toString());
            movement.setNotes("Sale checkout");
            movementRepository.save(movement);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleResponse getSaleById(UUID id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + id));
        return toResponse(sale);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getSales(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("'from' date cannot be after 'to' date");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        return saleRepository.findBySaleDateTimeBetween(start, end).stream()
                .sorted(Comparator.comparing(Sale::getSaleDateTime).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SaleResponse cancelSale(UUID id, UUID cancelledBy, String reason) {
        Sale sale = saleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + id));

        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessRuleException("Sale is already cancelled");
        }

        Map<String, Product> restoredProducts = new LinkedHashMap<>();
        for (SaleItem item : sale.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Product not found while cancelling sale: " + item.getProductId()));
            product.addStock(item.getQuantity());
            restoredProducts.put(product.getProductId(), product);

            StockMovement movement = new StockMovement();
            movement.setProductId(product.getProductId());
            movement.setCreatedBy(cancelledBy.toString());
            movement.setQuantityChange(item.getQuantity());
            movement.setMovementType(MovementType.ADJUSTMENT);
            movement.setReferenceId(sale.getSaleId().toString());
            movement.setNotes("Stock restored for cancelled sale: " + reason.trim());
            movementRepository.save(movement);
        }

        sale.cancel(reason);
        productRepository.saveAll(restoredProducts.values());
        saleRepository.save(sale);
        return toResponse(sale);
    }
    private SaleResponse toResponse(Sale sale) {
        Map<String, String> productNames = productRepository.findAllById(
                        sale.getItems().stream().map(SaleItem::getProductId).toList())
                .stream()
                .collect(Collectors.toMap(Product::getProductId, Product::getName));
        String cashierName = userRepository.findById(sale.getUserId().toString())
                .map(User::getUsername)
                .orElse("Unknown user");
        return SaleResponse.from(sale, cashierName, productNames);
    }
}
