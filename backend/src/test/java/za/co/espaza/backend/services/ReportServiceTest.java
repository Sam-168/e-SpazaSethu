package za.co.espaza.backend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.espaza.backend.dto.response.TopProductResponse;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.entity.Sale;
import za.co.espaza.backend.entity.SaleItem;
import za.co.espaza.backend.enums.PaymentMethod;
import za.co.espaza.backend.enums.SaleStatus;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.repository.ProductRepository;
import za.co.espaza.backend.repository.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(saleRepository, productRepository);
    }

    @Test
    void topProductsRanksQuantityAndRevenueIndependently() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        Sale sale = completedSale(
                new SaleItem("bread", 10, new BigDecimal("5.00")),
                new SaleItem("coffee", 2, new BigDecimal("40.00")));

        when(saleRepository.findBySaleDateTimeBetweenAndStatus(
                from.atStartOfDay(), to.atTime(LocalTime.MAX), SaleStatus.COMPLETED))
                .thenReturn(List.of(sale));
        when(productRepository.findAllById(any()))
                .thenReturn(List.of(product("bread", "Bread"), product("coffee", "Coffee")));

        List<TopProductResponse> byQuantity = reportService.topProductsByQuantity(from, to);
        List<TopProductResponse> byRevenue = reportService.topProductsByRevenue(from, to);

        assertThat(byQuantity).extracting(TopProductResponse::productId)
                .containsExactly("bread", "coffee");
        assertThat(byRevenue).extracting(TopProductResponse::productId)
                .containsExactly("coffee", "bread");
        assertThat(byQuantity.get(0).quantitySold()).isEqualTo(10);
        assertThat(byRevenue.get(0).revenue()).isEqualByComparingTo("80.00");

        verify(saleRepository, times(2)).findBySaleDateTimeBetweenAndStatus(
                from.atStartOfDay(), to.atTime(LocalTime.MAX), SaleStatus.COMPLETED);
    }

    @Test
    void topProductsReturnsEmptyListForEmptyPeriod() {
        LocalDate date = LocalDate.of(2026, 10, 15);
        when(saleRepository.findBySaleDateTimeBetweenAndStatus(
                date.atStartOfDay(), date.atTime(LocalTime.MAX), SaleStatus.COMPLETED))
                .thenReturn(List.of());

        assertThat(reportService.topProductsByQuantity(date, date)).isEmpty();
    }

    @Test
    void topProductsRejectsReversedDateRange() {
        LocalDate from = LocalDate.of(2026, 10, 31);
        LocalDate to = LocalDate.of(2026, 10, 1);

        assertThatThrownBy(() -> reportService.topProductsByRevenue(from, to))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be after");
    }

    private Sale completedSale(SaleItem... items) {
        Sale sale = new Sale(UUID.randomUUID(), PaymentMethod.CASH, SaleStatus.COMPLETED, null);
        for (SaleItem item : items) {
            sale.addItem(item);
        }
        sale.calculateTotal();
        return sale;
    }

    private Product product(String id, String name) {
        Product product = new Product();
        product.setProductId(id);
        product.setName(name);
        return product;
    }
}
