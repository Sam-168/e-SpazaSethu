package za.co.espaza.backend.dto.response;

import za.co.espaza.backend.entity.StocktakeItem;

import java.util.UUID;

public record StocktakeItemResponse(
        UUID stocktakeItemId,
        String productId,
        String productName,
        String barcode,
        int systemQuantity,
        Integer countedQuantity,
        Integer discrepancy,
        boolean adjusted
) {

    public static StocktakeItemResponse from(StocktakeItem item) {
        return from(item, null, null);
    }

    public static StocktakeItemResponse from(StocktakeItem item, String productName, String barcode) {
        return new StocktakeItemResponse(
                item.getStocktakeItemId(),
                item.getProductId(),
                productName,
                barcode,
                item.getSystemQuantity(),
                item.getCountedQuantity(),
                item.getDiscrepancy(),
                item.isAdjusted()
        );
    }
}
