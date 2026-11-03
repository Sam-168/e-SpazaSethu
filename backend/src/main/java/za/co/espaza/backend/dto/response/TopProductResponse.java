package za.co.espaza.backend.dto.response;

import java.math.BigDecimal;

/**
 * Aggregated product performance for a reporting date range.
 */
public record TopProductResponse(
        String productId,
        String productName,
        long quantitySold,
        BigDecimal revenue
) {
}
