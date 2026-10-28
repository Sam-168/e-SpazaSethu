package za.co.espaza.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.dto.response.DashboardReportResponse;
import za.co.espaza.backend.dto.response.SalesSummaryResponse;
import za.co.espaza.backend.dto.response.TopProductResponse;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private static final int TOP_PRODUCT_LIMIT = 5;

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
        validateDateRange(from, to);
        List<Sale> sales = completedSales(from, to);
        return new SalesSummaryResponse(totalSales(sales), totalProfit(sales));
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProductsByQuantity(LocalDate from, LocalDate to) {
        return topProducts(from, to, Comparator
                .comparingLong(TopProductResponse::quantitySold)
                .reversed()
                .thenComparing(TopProductResponse::productName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TopProductResponse::productId));
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProductsByRevenue(LocalDate from, LocalDate to) {
        return topProducts(from, to, Comparator
                .comparing(TopProductResponse::revenue)
                .reversed()
                .thenComparing(TopProductResponse::productName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TopProductResponse::productId));
    }

    private List<Sale> completedSales(LocalDate from, LocalDate to) {
        return saleRepository.findBySaleDateTimeBetweenAndStatus(
                from.atStartOfDay(), to.atTime(LocalTime.MAX), SaleStatus.COMPLETED);
    }

    private List<TopProductResponse> topProducts(
            LocalDate from,
            LocalDate to,
            Comparator<TopProductResponse> ordering) {
        validateDateRange(from, to);
        List<Sale> sales = completedSales(from, to);

        Map<String, ProductTotals> totalsByProduct = sales.stream()
                .flatMap(sale -> sale.getItems().stream())
                .collect(Collectors.toMap(
                        SaleItem::getProductId,
                        item -> new ProductTotals(item.getQuantity(), item.getSubtotal()),
                        ProductTotals::add));

        if (totalsByProduct.isEmpty()) {
            return List.of();
        }

        Map<String, String> productNames = productRepository.findAllById(totalsByProduct.keySet())
                .stream()
                .collect(Collectors.toMap(Product::getProductId, Product::getName));

        return totalsByProduct.entrySet().stream()
                .map(entry -> new TopProductResponse(
                        entry.getKey(),
                        productNames.getOrDefault(entry.getKey(), "Unknown product"),
                        entry.getValue().quantitySold(),
                        entry.getValue().revenue()))
                .sorted(ordering)
                .limit(TOP_PRODUCT_LIMIT)
                .toList();
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("'from' date cannot be after 'to' date");
        }
    }

    private BigDecimal totalSales(List<Sale> sales) {
        return sales.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalProfit(List<Sale> sales) {
        List<String> ids = sales.stream().flatMap(sale -> sale.getItems().stream())
                .map(SaleItem::getProductId).distinct().toList();
        Map<String, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        return sales.stream().flatMap(sale -> sale.getItems().stream()).map(item -> {
            Product product = products.get(item.getProductId());
            BigDecimal cost = product == null || product.getCostPrice() == null
                    ? BigDecimal.ZERO
                    : product.getCostPrice();
            return item.getUnitPrice().subtract(cost).multiply(BigDecimal.valueOf(item.getQuantity()));
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record ProductTotals(long quantitySold, BigDecimal revenue) {
        private ProductTotals add(ProductTotals other) {
            return new ProductTotals(
                    quantitySold + other.quantitySold,
                    revenue.add(other.revenue));
        }
    }
}
