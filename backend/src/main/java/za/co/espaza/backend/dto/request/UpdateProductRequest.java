package za.co.espaza.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class UpdateProductRequest {
    // Partial update: null means "leave unchanged". @NotBlank would also reject
    // null (making name mandatory), so @Pattern is used instead — it passes for
    // null but rejects empty/whitespace-only names.
    @Pattern(regexp = ".*\\S.*", message = "Product name must not be blank")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "Barcode must be alphanumeric")
    private String barcode;

    @Min(value = 0, message = "Selling price must be 0 or more")
    private BigDecimal sellingPrice;
    @Min(value = 0, message = "Cost price must be 0 or more")
    private BigDecimal costPrice;
    @Min(value = 0, message = "Low stock threshold must be 0 or more")
    private Integer lowStockThreshold;
    private String categoryId;
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

}
