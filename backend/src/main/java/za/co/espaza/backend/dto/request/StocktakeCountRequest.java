package za.co.espaza.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StocktakeCountRequest(
        @NotNull(message = "Stocktake item id is required")
        UUID stocktakeItemId,
        // Null means the item was skipped; 0 is a valid count (all units missing).
        @Min(value = 0, message = "Counted quantity must be 0 or more")
        Integer countedQuantity) {
}
