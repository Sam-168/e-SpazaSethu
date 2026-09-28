package za.co.espaza.backend.dto.request;

import za.co.espaza.backend.Enum.MovementType;

import java.time.LocalDateTime;

public class MovementFilterRequest {
    private LocalDateTime from;
    private LocalDateTime to;
    private String productId;
    private MovementType type;

    public MovementFilterRequest() {

    }
    public LocalDateTime getFrom() {
        return from;
    }
    public void setFrom(LocalDateTime from) {
        this.from = from;
    }
    public LocalDateTime getTo() {
        return to;
    }
    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public MovementType getType() {
        return type;
    }

    public void setType(MovementType type) {
        this.type = type;
    }
}
