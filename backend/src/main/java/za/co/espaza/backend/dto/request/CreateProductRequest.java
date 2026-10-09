package za.co.espaza.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateProductRequest {
    @NotBlank(message = "Product name is required")
    private String name;
    private String barcode;
    @NotNull(message = "Selling price is required")
    @Min(value = 0, message = "Selling price must be 0 or more")
    private BigDecimal sellingPrice;
    @NotNull(message = "Cost price is required")
    @Min(value = 0, message = "Cost price must be 0 or more")
    private BigDecimal costPrice;
    @Min(value = 0, message = "Stock quantity must be 0 or more")
    private Integer stockQuantity;
    // Optional: a null threshold means the product uses the default low-stock threshold.
    @Min(value = 0, message = "Low stock threshold must be 0 or more")
    private Integer lowStockThreshold;
    private String categoryId;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
