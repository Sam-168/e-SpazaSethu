package za.co.espaza.backend.dto.response;

import za.co.espaza.backend.Enum.MovementType;

import java.time.LocalDateTime;

public class StockMovementResponse {
    private String movementId;
    private String productId;
    private MovementType movementType;
    private Integer quantityChange;
    private String referenceId;
    private String userId;
    private String notes;
    private LocalDateTime createdAt;

    public StockMovementResponse(String movementId, String productId, MovementType movementType, Integer quantityChange, String referenceId, String userId, String notes, LocalDateTime createdAt){
        this.movementId = movementId;
        this.productId = productId;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.referenceId = referenceId;
        this.userId = userId;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getMovementId() {
        return movementId;
    }

    public String getProductId() {
        return productId;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getUserId() {
        return userId;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
