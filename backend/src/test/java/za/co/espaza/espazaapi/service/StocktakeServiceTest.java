package za.co.espaza.espazaapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.espaza.backend.Enum.MovementType;
import za.co.espaza.backend.dto.response.StocktakeResponse;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.entity.StockMovement;
import za.co.espaza.backend.entity.Stocktake;
import za.co.espaza.backend.entity.StocktakeItem;
import za.co.espaza.backend.enums.StocktakeStatus;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.repository.ProductRepository;
import za.co.espaza.backend.repository.StockMovementRepository;
import za.co.espaza.backend.repository.StocktakeRepository;
import za.co.espaza.backend.services.StocktakeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StocktakeServiceTest {

    @Mock
    private StocktakeRepository stocktakeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @InjectMocks
    private StocktakeService stocktakeService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private Product createProduct(String id, String name, int stockQuantity, boolean isActive) {
        Product product = new Product();
        product.setProductId(id);
        product.setName(name);
        product.setSellingPrice(new BigDecimal("20.00"));
        product.setCostPrice(new BigDecimal("12.00"));
        product.setStockQuantity(stockQuantity);
        product.setIsActive(isActive);
        return product;
    }

    @Test
    void startStocktake_success() {
        // Arrange
        Product sugar = createProduct("prod-sugar", "Brown Sugar", 15, true);
        Product flour = createProduct("prod-flour", "Cake Flour", 42, true);

        when(stocktakeRepository.findByStatusAndConductedBy(StocktakeStatus.IN_PROGRESS, userId))
                .thenReturn(Optional.empty());
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(sugar, flour));
        when(stocktakeRepository.save(any(Stocktake.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(sugar, flour));

        // Act
        StocktakeResponse response = stocktakeService.startStocktake(userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StocktakeStatus.IN_PROGRESS);

        ArgumentCaptor<Stocktake> captor = ArgumentCaptor.forClass(Stocktake.class);
        verify(stocktakeRepository).save(captor.capture());
        Stocktake savedStocktake = captor.getValue();

        assertThat(savedStocktake.getStatus()).isEqualTo(StocktakeStatus.IN_PROGRESS);
        assertThat(savedStocktake.getItems()).hasSize(2);

        StocktakeItem sugarItem = savedStocktake.getItems().stream()
                .filter(i -> "prod-sugar".equals(i.getProductId()))
                .findFirst()
                .orElseThrow();
        assertThat(sugarItem.getSystemQuantity()).isEqualTo(15);
        assertThat(sugarItem.getCountedQuantity()).isNull();

        StocktakeItem flourItem = savedStocktake.getItems().stream()
                .filter(i -> "prod-flour".equals(i.getProductId()))
                .findFirst()
                .orElseThrow();
        assertThat(flourItem.getSystemQuantity()).isEqualTo(42);
        assertThat(flourItem.getCountedQuantity()).isNull();
    }

    @Test
    void startStocktake_alreadyInProgress() {
        // Arrange
        Stocktake ongoing = new Stocktake(userId, "Active count");
        when(stocktakeRepository.findByStatusAndConductedBy(StocktakeStatus.IN_PROGRESS, userId))
                .thenReturn(Optional.of(ongoing));

        // Act
        Throwable thrown = catchThrowable(() -> stocktakeService.startStocktake(userId));

        // Assert
        assertThat(thrown)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already in progress");

        verify(stocktakeRepository, never()).save(any());
        verify(productRepository, never()).findByIsActiveTrue();
    }

    @Test
    void applyAdjustments_updatesStock() {
        // Arrange
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(userId, "Audit adjust");
        StocktakeItem item = new StocktakeItem("prod-rice", 10);
        item.setCountedQuantity(8); // discrepancy: -2
        stocktake.addItem(item);

        Product product = createProduct("prod-rice", "Jasmine Rice", 10, true);

        when(stocktakeRepository.findById(stocktakeId)).thenReturn(Optional.of(stocktake));
        when(productRepository.findByIdForUpdate("prod-rice")).thenReturn(Optional.of(product));
        when(stocktakeRepository.save(any(Stocktake.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(product));

        // Act
        stocktakeService.applyAdjustments(stocktakeId, userId);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(8);
        verify(productRepository).save(product);
    }

    @Test
    void applyAdjustments_createsMovements() {
        // Arrange
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(userId, "Audit adjust multiple");

        StocktakeItem item1 = new StocktakeItem("prod-1", 10);
        item1.setCountedQuantity(7); // discrepancy: -3
        stocktake.addItem(item1);

        StocktakeItem item2 = new StocktakeItem("prod-2", 5);
        item2.setCountedQuantity(9); // discrepancy: +4
        stocktake.addItem(item2);

        StocktakeItem item3 = new StocktakeItem("prod-3", 8);
        item3.setCountedQuantity(8); // discrepancy: 0 (not discrepant)
        stocktake.addItem(item3);

        Product prod1 = createProduct("prod-1", "Product 1", 10, true);
        Product prod2 = createProduct("prod-2", "Product 2", 5, true);

        when(stocktakeRepository.findById(stocktakeId)).thenReturn(Optional.of(stocktake));
        when(productRepository.findByIdForUpdate("prod-1")).thenReturn(Optional.of(prod1));
        when(productRepository.findByIdForUpdate("prod-2")).thenReturn(Optional.of(prod2));
        when(stocktakeRepository.save(any(Stocktake.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(prod1, prod2));

        // Act
        stocktakeService.applyAdjustments(stocktakeId, userId);

        // Assert
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository, times(2)).save(movementCaptor.capture());
        List<StockMovement> movements = movementCaptor.getAllValues();

        assertThat(movements).extracting(StockMovement::getProductId)
                .containsExactlyInAnyOrder("prod-1", "prod-2");
        assertThat(movements).extracting(StockMovement::getQuantityChange)
                .containsExactlyInAnyOrder(-3, 4);
        assertThat(movements).allMatch(m -> m.getMovementType() == MovementType.STOCKTAKE_CORRECTION);
        assertThat(movements).allMatch(m -> stocktakeId.toString().equals(m.getReferenceId()));
        assertThat(movements).allMatch(m -> userId.toString().equals(m.getCreatedBy()));

        verify(productRepository, never()).findByIdForUpdate("prod-3");
    }

    @Test
    void applyAdjustments_skipsNullCounts() {
        // Arrange
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(userId, "Audit adjust skip null");

        StocktakeItem item1 = new StocktakeItem("prod-uncounted", 10);
        // countedQuantity is null (skipped count)
        stocktake.addItem(item1);

        StocktakeItem item2 = new StocktakeItem("prod-counted", 5);
        item2.setCountedQuantity(8); // discrepancy: +3
        stocktake.addItem(item2);

        Product prod2 = createProduct("prod-counted", "Counted Product", 5, true);

        when(stocktakeRepository.findById(stocktakeId)).thenReturn(Optional.of(stocktake));
        when(productRepository.findByIdForUpdate("prod-counted")).thenReturn(Optional.of(prod2));
        when(stocktakeRepository.save(any(Stocktake.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(anyList())).thenReturn(List.of(prod2));

        // Act
        stocktakeService.applyAdjustments(stocktakeId, userId);

        // Assert
        assertThat(item1.isAdjusted()).isFalse();
        assertThat(item1.getCountedQuantity()).isNull();
        verify(productRepository, never()).findByIdForUpdate("prod-uncounted");

        assertThat(item2.isAdjusted()).isTrue();
        verify(productRepository).findByIdForUpdate("prod-counted");
        verify(movementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    void applyAdjustments_completesStocktake() {
        // Arrange
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(userId, "Audit adjust complete");

        when(stocktakeRepository.findById(stocktakeId)).thenReturn(Optional.of(stocktake));
        when(stocktakeRepository.save(any(Stocktake.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(anyList())).thenReturn(List.of());

        // Act
        StocktakeResponse response = stocktakeService.applyAdjustments(stocktakeId, userId);

        // Assert
        assertThat(response.status()).isEqualTo(StocktakeStatus.COMPLETED);

        ArgumentCaptor<Stocktake> captor = ArgumentCaptor.forClass(Stocktake.class);
        verify(stocktakeRepository).save(captor.capture());
        Stocktake savedStocktake = captor.getValue();

        assertThat(savedStocktake.getStatus()).isEqualTo(StocktakeStatus.COMPLETED);
        assertThat(savedStocktake.getCompletedAt()).isNotNull();
    }
}
