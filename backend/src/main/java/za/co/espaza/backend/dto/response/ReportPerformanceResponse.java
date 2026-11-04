package za.co.espaza.backend.dto.response;

import za.co.espaza.backend.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportPerformanceResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalSales,
        BigDecimal totalProfit,
        long transactionCount,
        long unitsSold,
        BigDecimal averageSale,
        List<DailySales> dailySales,
        List<PaymentBreakdown> payments,
        List<ProductPerformance> topProducts,
        List<ProductPerformance> topProductsByQuantity,
        List<ProductPerformance> topProductsByRevenue,
        List<LowStockProduct> lowStockProducts) {

    public record DailySales(LocalDate date, long transactionCount, BigDecimal totalSales, BigDecimal totalProfit) { }

    public record PaymentBreakdown(PaymentMethod paymentMethod, long transactionCount, BigDecimal totalAmount) { }

    public record ProductPerformance(
            String productId, String productName, long quantitySold, BigDecimal revenue, BigDecimal profit) { }

    public record LowStockProduct(
            String productId, String productName, int stockQuantity, int lowStockThreshold) { }
}
