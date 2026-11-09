package za.co.espaza.backend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StocktakeServiceTest {

    @Mock private StocktakeRepository stocktakeRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMovementRepository movementRepository;
    private StocktakeService service;

    @BeforeEach
    void setUp() {
        service = new StocktakeService(stocktakeRepository, productRepository, movementRepository);
    }

    @Test
    void startStocktakeSnapshotsEveryActiveProduct() {
        UUID userId = UUID.randomUUID();
        Product product = product("p-1", "Maize Meal 2.5kg", "600100000001", 12);
        when(stocktakeRepository.findFirstByStatusOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product));
        when(stocktakeRepository.save(any(Stocktake.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        StocktakeResponse response = service.startStocktake(userId);

        assertEquals(1, response.items().size());
        assertEquals(12, response.items().get(0).systemQuantity());
        assertEquals("600100000001", response.items().get(0).barcode());
        assertEquals(StocktakeStatus.IN_PROGRESS, response.status());
    }

    @Test
    void startStocktakeRejectsAnotherShopWideSession() {
        when(stocktakeRepository.findFirstByStatusOrderByStartedAtDesc(StocktakeStatus.IN_PROGRESS))
                .thenReturn(Optional.of(new Stocktake(UUID.randomUUID(), null)));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.startStocktake(UUID.randomUUID()));

        assertEquals("A stocktake is already in progress", error.getMessage());
        verify(productRepository, never()).findByIsActiveTrue();
    }

    @Test
    void newlyRegisteredProductCanBeIncludedOnceDuringActiveStocktake() {
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(UUID.randomUUID(), null);
        Product original = product("p-1", "Bread", "600100000001", 4);
        Product discovered = product("p-2", "Sprite 500ml", "5449000332967", 0);
        stocktake.addItem(new StocktakeItem(original.getProductId(), 4));
        when(stocktakeRepository.findById(stocktakeId)).thenReturn(Optional.of(stocktake));
        when(productRepository.findById("p-2")).thenReturn(Optional.of(discovered));
        when(stocktakeRepository.save(stocktake)).thenReturn(stocktake);
        when(productRepository.findAllById(any())).thenReturn(List.of(original, discovered));

        StocktakeResponse added = service.addProduct(stocktakeId, "p-2");
        StocktakeResponse repeated = service.addProduct(stocktakeId, "p-2");

        assertEquals(2, added.items().size());
        assertEquals(2, repeated.items().size());
        assertEquals(0, added.items().stream()
                .filter(item -> "5449000332967".equals(item.barcode()))
                .findFirst().orElseThrow().systemQuantity());
        verify(stocktakeRepository).save(stocktake);
    }

    @Test
    void applyAdjustmentsRejectsUncountedProducts() {
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(UUID.randomUUID(), null);
        stocktake.addItem(new StocktakeItem("p-1", 10));
        when(stocktakeRepository.findById(stocktakeId)).thenReturn(Optional.of(stocktake));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.applyAdjustments(stocktakeId, UUID.randomUUID()));

        assertEquals("Count every product before applying the stocktake", error.getMessage());
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void applyAdjustmentsAuditsTheActualChangeFromCurrentStock() {
        UUID stocktakeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(userId, null);
        StocktakeItem item = new StocktakeItem("p-1", 10);
        item.setCountedQuantity(8);
        stocktake.addItem(item);
        Product product = product("p-1", "Maize Meal 2.5kg", "600100000001", 9);
        when(stocktakeRepository.findById(stocktakeId)).thenReturn(Optional.of(stocktake));
        when(productRepository.findByIdForUpdate("p-1")).thenReturn(Optional.of(product));
        when(stocktakeRepository.save(stocktake)).thenReturn(stocktake);
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        StocktakeResponse response = service.applyAdjustments(stocktakeId, userId);

        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(-1, movement.getValue().getQuantityChange());
        assertEquals(MovementType.STOCKTAKE_CORRECTION, movement.getValue().getMovementType());
        assertEquals(8, product.getStockQuantity());
        assertTrue(item.isAdjusted());
        assertEquals(StocktakeStatus.COMPLETED, response.status());
    }

    private Product product(String id, String name, String barcode, int stock) {
        Product product = new Product();
        product.setProductId(id);
        product.setName(name);
        product.setBarcode(barcode);
        product.setStockQuantity(stock);
        product.setIsActive(true);
        return product;
    }
}
