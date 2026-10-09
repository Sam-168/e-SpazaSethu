package za.co.espaza.espazaapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.espaza.backend.Enum.MovementType;
import za.co.espaza.backend.dto.request.CreateSaleItemRequest;
import za.co.espaza.backend.dto.request.CreateSaleRequest;
import za.co.espaza.backend.dto.response.SaleResponse;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.entity.Sale;
import za.co.espaza.backend.entity.SaleItem;
import za.co.espaza.backend.entity.StockMovement;
import za.co.espaza.backend.entity.User;
import za.co.espaza.backend.enums.PaymentMethod;
import za.co.espaza.backend.enums.SaleStatus;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.exception.EntityNotFoundException;
import za.co.espaza.backend.repository.ProductRepository;
import za.co.espaza.backend.repository.SaleRepository;
import za.co.espaza.backend.repository.StockMovementRepository;
import za.co.espaza.backend.repository.UserRepository;
import za.co.espaza.backend.services.SaleService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SaleService saleService;

    private UUID userId;
    private User cashierUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cashierUser = new User();
        cashierUser.setUserId(userId.toString());
        cashierUser.setUsername("test-cashier");
    }

    private Product createProduct(String id, String name, BigDecimal price, int stockQuantity, boolean isActive) {
        Product product = new Product();
        product.setProductId(id);
        product.setName(name);
        product.setSellingPrice(price);
        product.setCostPrice(price.multiply(BigDecimal.valueOf(0.7)));
        product.setStockQuantity(stockQuantity);
        product.setIsActive(isActive);
        return product;
    }

    @Test
    void completeSale_success() {
        // Arrange
        Product bread = createProduct("prod-bread", "White Bread", new BigDecimal("15.00"), 10, true);
        Product milk = createProduct("prod-milk", "Fresh Milk", new BigDecimal("25.00"), 20, true);

        CreateSaleRequest request = new CreateSaleRequest(
                PaymentMethod.CASH,
                List.of(
                        new CreateSaleItemRequest("prod-bread", 2),
                        new CreateSaleItemRequest("prod-milk", 3)
                ),
                "Standard checkout"
        );

        when(productRepository.findByIdForUpdate("prod-bread")).thenReturn(Optional.of(bread));
        when(productRepository.findByIdForUpdate("prod-milk")).thenReturn(Optional.of(milk));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(bread, milk));
        when(userRepository.findById(userId.toString())).thenReturn(Optional.of(cashierUser));

        // Act
        SaleResponse response = saleService.completeSale(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(SaleStatus.COMPLETED);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("105.00"));

        // Verify stock deducted
        assertThat(bread.getStockQuantity()).isEqualTo(8);
        assertThat(milk.getStockQuantity()).isEqualTo(17);

        // Verify product changes saved
        verify(productRepository).saveAll(any());

        // Verify sale persisted
        verify(saleRepository).saveAndFlush(any(Sale.class));

        // Verify movements created
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository, times(2)).save(movementCaptor.capture());
        List<StockMovement> savedMovements = movementCaptor.getAllValues();

        assertThat(savedMovements).extracting(StockMovement::getProductId)
                .containsExactlyInAnyOrder("prod-bread", "prod-milk");
        assertThat(savedMovements).extracting(StockMovement::getQuantityChange)
                .containsExactlyInAnyOrder(-2, -3);
        assertThat(savedMovements).allMatch(m -> m.getMovementType() == MovementType.SALE);
        assertThat(savedMovements).allMatch(m -> userId.toString().equals(m.getCreatedBy()));
    }

    @Test
    void completeSale_insufficientStock() {
        // Arrange
        Product soda = createProduct("prod-soda", "Soda Can", new BigDecimal("12.00"), 2, true);

        CreateSaleRequest request = new CreateSaleRequest(
                PaymentMethod.CASH,
                List.of(new CreateSaleItemRequest("prod-soda", 5)),
                "Order exceeding stock"
        );

        when(productRepository.findByIdForUpdate("prod-soda")).thenReturn(Optional.of(soda));

        // Act & Assert
        assertThatThrownBy(() -> saleService.completeSale(request, userId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");

        verify(saleRepository, never()).saveAndFlush(any());
        verify(movementRepository, never()).save(any());
        verify(productRepository, never()).saveAll(any());
    }

    @Test
    void completeSale_stockDeductionDoesNotPartiallyCommit() {
        // Arrange
        Product item1 = createProduct("prod-1", "Item One", new BigDecimal("10.00"), 10, true);
        Product item2 = createProduct("prod-2", "Item Two", new BigDecimal("20.00"), 2, true);

        CreateSaleRequest request = new CreateSaleRequest(
                PaymentMethod.CASH,
                List.of(
                        new CreateSaleItemRequest("prod-1", 3),
                        new CreateSaleItemRequest("prod-2", 5)
                ),
                "Partial stock fail"
        );

        when(productRepository.findByIdForUpdate("prod-1")).thenReturn(Optional.of(item1));
        when(productRepository.findByIdForUpdate("prod-2")).thenReturn(Optional.of(item2));

        // Act & Assert
        assertThatThrownBy(() -> saleService.completeSale(request, userId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");

        // First item's stock must NOT be deducted
        assertThat(item1.getStockQuantity()).isEqualTo(10);
        // Second item's stock must NOT be deducted
        assertThat(item2.getStockQuantity()).isEqualTo(2);

        verify(productRepository, never()).saveAll(any());
        verify(saleRepository, never()).saveAndFlush(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void completeSale_emptyCart() {
        // Arrange
        CreateSaleRequest request = new CreateSaleRequest(
                PaymentMethod.CASH,
                List.of(),
                "Empty cart sale"
        );

        // Act & Assert
        assertThatThrownBy(() -> saleService.completeSale(request, userId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("A sale must contain at least one item");

        verifyNoInteractions(productRepository, saleRepository, movementRepository);
    }

    @Test
    void completeSale_productNotFound() {
        // Arrange
        CreateSaleRequest request = new CreateSaleRequest(
                PaymentMethod.CASH,
                List.of(new CreateSaleItemRequest("non-existent-product", 1)),
                "Invalid product"
        );

        when(productRepository.findByIdForUpdate("non-existent-product")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> saleService.completeSale(request, userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found: non-existent-product");

        verify(saleRepository, never()).saveAndFlush(any());
        verify(movementRepository, never()).save(any());
        verify(productRepository, never()).saveAll(any());
    }

    @Test
    void completeSale_totalEqualsSum() {
        // Arrange
        Product prod1 = createProduct("p1", "Product 1", new BigDecimal("12.50"), 10, true);
        Product prod2 = createProduct("p2", "Product 2", new BigDecimal("25.00"), 10, true);
        Product prod3 = createProduct("p3", "Product 3", new BigDecimal("9.99"), 10, true);

        CreateSaleRequest request = new CreateSaleRequest(
                PaymentMethod.CARD,
                List.of(
                        new CreateSaleItemRequest("p1", 2),
                        new CreateSaleItemRequest("p2", 3),
                        new CreateSaleItemRequest("p3", 1)
                ),
                "Total verification"
        );

        when(productRepository.findByIdForUpdate("p1")).thenReturn(Optional.of(prod1));
        when(productRepository.findByIdForUpdate("p2")).thenReturn(Optional.of(prod2));
        when(productRepository.findByIdForUpdate("p3")).thenReturn(Optional.of(prod3));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(prod1, prod2, prod3));
        when(userRepository.findById(userId.toString())).thenReturn(Optional.of(cashierUser));

        // Act
        SaleResponse response = saleService.completeSale(request, userId);

        // Assert
        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).saveAndFlush(saleCaptor.capture());
        Sale savedSale = saleCaptor.getValue();

        BigDecimal expectedTotal = new BigDecimal("109.99");
        BigDecimal calculatedSumOfSubtotals = savedSale.getItems().stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(savedSale.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(savedSale.getTotalAmount()).isEqualByComparingTo(calculatedSumOfSubtotals);
        assertThat(response.totalAmount()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void completeSale_unitPriceSnapshotted() {
        // Arrange
        BigDecimal originalPrice = new BigDecimal("45.50");
        Product product = createProduct("snap-prod", "Snapshot Item", originalPrice, 10, true);

        CreateSaleRequest request = new CreateSaleRequest(
                PaymentMethod.CASH,
                List.of(new CreateSaleItemRequest("snap-prod", 2)),
                "Snapshot test"
        );

        when(productRepository.findByIdForUpdate("snap-prod")).thenReturn(Optional.of(product));
        when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));
        when(userRepository.findById(userId.toString())).thenReturn(Optional.of(cashierUser));

        // Act
        saleService.completeSale(request, userId);

        // Simulate subsequent product price increase after checkout
        product.setSellingPrice(new BigDecimal("99.99"));

        // Assert
        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).saveAndFlush(saleCaptor.capture());
        Sale savedSale = saleCaptor.getValue();

        assertThat(savedSale.getItems()).hasSize(1);
        SaleItem item = savedSale.getItems().get(0);
        assertThat(item.getUnitPrice()).isEqualByComparingTo(originalPrice);
        assertThat(item.getUnitPrice()).isNotEqualByComparingTo(product.getSellingPrice());
    }
}
