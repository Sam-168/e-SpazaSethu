package za.co.espaza.backend.dto.response;

import za.co.espaza.backend.Enum.MovementType;
import java.time.LocalDateTime;

public record StockMovementResponse(
        String movementId,
        String productId,
        String productName,
        MovementType movementType,
        Integer quantityChange,
        String referenceId,
        String userId,
        String username,
        String notes,
        LocalDateTime createdAt
) {
}
