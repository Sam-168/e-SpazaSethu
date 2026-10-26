package za.co.espaza.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.dto.response.DashboardReportResponse;
import za.co.espaza.backend.dto.response.ReportPerformanceResponse;
import za.co.espaza.backend.dto.response.SalesSummaryResponse;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.entity.Sale;
import za.co.espaza.backend.entity.SaleItem;
import za.co.espaza.backend.enums.SaleStatus;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.repository.ProductRepository;
import za.co.espaza.backend.repository.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public ReportService(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public DashboardReportResponse dashboard() {
        LocalDate today = LocalDate.now();
        List<Sale> sales = completedSales(today, today);
        return new DashboardReportResponse(totalSales(sales), totalProfit(sales), sales.size());
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse summary(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<Sale> sales = completedSales(from, to);
        return new SalesSummaryResponse(totalSales(sales), totalProfit(sales));
    }

    @Transactional(readOnly = true)
    public ReportPerformanceResponse performance(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<Sale> sales = completedSales(from, to);
        Map<String, Product> products = productsFor(sales);

        BigDecimal salesTotal = totalSales(sales);
        BigDecimal profitTotal = totalProfit(sales, products);
        long unitsSold = sales.stream().flatMap(sale -> sale.getItems().stream())
                .mapToLong(SaleItem::getQuantity).sum();
        BigDecimal averageSale = sales.isEmpty() ? BigDecimal.ZERO
                : salesTotal.divide(BigDecimal.valueOf(sales.size()), 2, java.math.RoundingMode.HALF_UP);

        Map<LocalDate, MutableDaily> days = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.put(date, new MutableDaily());
        }
        Map<za.co.espaza.backend.enums.PaymentMethod, MutablePayment> payments =
                new EnumMap<>(za.co.espaza.backend.enums.PaymentMethod.class);
        Map<String, MutableProduct> productRows = new LinkedHashMap<>();

        for (Sale sale : sales) {
            MutableDaily day = days.get(sale.getSaleDateTime().toLocalDate());
            day.transactionCount++;
            day.totalSales = day.totalSales.add(sale.getTotalAmount());

            MutablePayment payment = payments.computeIfAbsent(sale.getPaymentMethod(), ignored -> new MutablePayment());
            payment.transactionCount++;
            payment.totalAmount = payment.totalAmount.add(sale.getTotalAmount());

            for (SaleItem item : sale.getItems()) {
                Product product = products.get(item.getProductId());
                BigDecimal itemProfit = item.getUnitPrice().subtract(costOf(product))
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                day.totalProfit = day.totalProfit.add(itemProfit);

                MutableProduct row = productRows.computeIfAbsent(item.getProductId(), ignored ->
                        new MutableProduct(product == null ? "Unknown product" : product.getName()));
                row.quantitySold += item.getQuantity();
                row.revenue = row.revenue.add(item.getSubtotal());
                row.profit = row.profit.add(itemProfit);
            }
        }

        List<ReportPerformanceResponse.DailySales> dailySales = days.entrySet().stream()
                .map(entry -> new ReportPerformanceResponse.DailySales(entry.getKey(),
                        entry.getValue().transactionCount, entry.getValue().totalSales, entry.getValue().totalProfit))
                .toList();
        List<ReportPerformanceResponse.PaymentBreakdown> paymentRows = payments.entrySet().stream()
                .map(entry -> new ReportPerformanceResponse.PaymentBreakdown(entry.getKey(),
                        entry.getValue().transactionCount, entry.getValue().totalAmount))
                .sorted(Comparator.comparing(ReportPerformanceResponse.PaymentBreakdown::totalAmount).reversed())
                .toList();
        List<ReportPerformanceResponse.ProductPerformance> topProducts = productRows.entrySet().stream()
                .map(entry -> new ReportPerformanceResponse.ProductPerformance(entry.getKey(), entry.getValue().name,
                        entry.getValue().quantitySold, entry.getValue().revenue, entry.getValue().profit))
                .sorted(Comparator.comparing(ReportPerformanceResponse.ProductPerformance::revenue).reversed())
                .limit(10)
                .toList();

        return new ReportPerformanceResponse(from, to, salesTotal, profitTotal, sales.size(), unitsSold,
                averageSale, dailySales, paymentRows, topProducts);
    }

    private List<Sale> completedSales(LocalDate from, LocalDate to) {
        return saleRepository.findBySaleDateTimeBetweenAndStatus(
                from.atStartOfDay(), to.atTime(LocalTime.MAX), SaleStatus.COMPLETED);
    }

    private BigDecimal totalSales(List<Sale> sales) {
        return sales.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalProfit(List<Sale> sales) {
        return totalProfit(sales, productsFor(sales));
    }

    private BigDecimal totalProfit(List<Sale> sales, Map<String, Product> products) {
        return sales.stream().flatMap(sale -> sale.getItems().stream()).map(item -> {
            Product product = products.get(item.getProductId());
            BigDecimal cost = costOf(product);
            return item.getUnitPrice().subtract(cost).multiply(BigDecimal.valueOf(item.getQuantity()));
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Product> productsFor(List<Sale> sales) {
        List<String> ids = sales.stream().flatMap(sale -> sale.getItems().stream())
                .map(SaleItem::getProductId).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
    }

    private BigDecimal costOf(Product product) {
        return product == null || product.getCostPrice() == null ? BigDecimal.ZERO : product.getCostPrice();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new BusinessRuleException("'from' date cannot be after 'to' date");
    }

    private static final class MutableDaily {
        private long transactionCount;
        private BigDecimal totalSales = BigDecimal.ZERO;
        private BigDecimal totalProfit = BigDecimal.ZERO;
    }

    private static final class MutablePayment {
        private long transactionCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
    }

    private static final class MutableProduct {
        private final String name;
        private long quantitySold;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;

        private MutableProduct(String name) { this.name = name; }
    }
}
