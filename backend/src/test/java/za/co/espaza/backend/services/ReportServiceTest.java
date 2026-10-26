package za.co.espaza.backend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.espaza.backend.dto.response.ReportPerformanceResponse;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private ProductRepository productRepository;
    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(saleRepository, productRepository);
    }

    @Test
    void performanceBuildsDailyPaymentAndProductTotalsFromCompletedSales() {
        LocalDate from = LocalDate.of(2026, 10, 20);
        LocalDate to = LocalDate.of(2026, 10, 21);
        SaleItem item = mock(SaleItem.class);
        when(item.getProductId()).thenReturn("p-1");
        when(item.getQuantity()).thenReturn(2);
        when(item.getUnitPrice()).thenReturn(new BigDecimal("15.00"));
        when(item.getSubtotal()).thenReturn(new BigDecimal("30.00"));

        Sale sale = mock(Sale.class);
        when(sale.getSaleDateTime()).thenReturn(LocalDateTime.of(2026, 10, 20, 10, 30));
        when(sale.getTotalAmount()).thenReturn(new BigDecimal("30.00"));
        when(sale.getPaymentMethod()).thenReturn(PaymentMethod.CASH);
        when(sale.getItems()).thenReturn(List.of(item));

        Product product = new Product();
        product.setProductId("p-1");
        product.setName("Milk 1L");
        product.setCostPrice(new BigDecimal("10.00"));
        when(saleRepository.findBySaleDateTimeBetweenAndStatus(
                from.atStartOfDay(), to.atTime(java.time.LocalTime.MAX), SaleStatus.COMPLETED))
                .thenReturn(List.of(sale));
        when(productRepository.findAllById(List.of("p-1"))).thenReturn(List.of(product));

        ReportPerformanceResponse report = service.performance(from, to);

        assertEquals(new BigDecimal("30.00"), report.totalSales());
        assertEquals(new BigDecimal("10.00"), report.totalProfit());
        assertEquals(new BigDecimal("30.00"), report.averageSale());
        assertEquals(1, report.transactionCount());
        assertEquals(2, report.unitsSold());
        assertEquals(2, report.dailySales().size());
        assertEquals(BigDecimal.ZERO, report.dailySales().get(1).totalSales());
        assertEquals(PaymentMethod.CASH, report.payments().get(0).paymentMethod());
        assertEquals("Milk 1L", report.topProducts().get(0).productName());
        assertEquals(2, report.topProducts().get(0).quantitySold());
    }

    @Test
    void performanceRejectsAnInvertedDateRange() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.performance(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 21)));

        assertEquals("'from' date cannot be after 'to' date", error.getMessage());
    }
}
