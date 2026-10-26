package za.co.espaza.backend.dto.response;

import za.co.espaza.backend.entity.Stocktake;
import za.co.espaza.backend.enums.StocktakeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StocktakeResponse(
        UUID stocktakeId,
        UUID conductedBy,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        StocktakeStatus status,
        String notes,
        List<StocktakeItemResponse> items
) {

    public static StocktakeResponse from(Stocktake stocktake) {
        return from(stocktake, Map.of(), Map.of());
    }

    public static StocktakeResponse from(Stocktake stocktake,
                                         Map<String, String> productNames,
                                         Map<String, String> productBarcodes) {
        List<StocktakeItemResponse> items = stocktake.getItems().stream()
                .map(item -> StocktakeItemResponse.from(
                        item,
                        productNames.get(item.getProductId()),
                        productBarcodes.get(item.getProductId())))
                .toList();

        return new StocktakeResponse(
                stocktake.getStocktakeId(),
                stocktake.getConductedBy(),
                stocktake.getStartedAt(),
                stocktake.getCompletedAt(),
                stocktake.getStatus(),
                stocktake.getNotes(),
                items
        );
    }
}
