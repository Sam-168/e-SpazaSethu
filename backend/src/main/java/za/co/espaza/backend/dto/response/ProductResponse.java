package za.co.espaza.backend.dto.response;

import za.co.espaza.backend.entity.Category;
import za.co.espaza.backend.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {
    private final String productId;
    private final String name;
    private final String barcode;
    private final BigDecimal sellingPrice;
    private final BigDecimal costPrice;
    private final Integer stockQuantity;
    private final Integer lowStockThreshold;
    private final boolean lowStock;
    private final String categoryId;
    private final String categoryName;
    private final String description;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ProductResponse(Product p) {
        Category c = p.getCategory();
        this.productId = p.getProductId();
        this.name = p.getName();
        this.barcode = p.getBarcode();
        this.sellingPrice = p.getSellingPrice();
        this.costPrice = p.getCostPrice();
        this.stockQuantity = p.getStockQuantity();
        this.lowStockThreshold = p.getLowStockThreshold();
        this.lowStock = p.isLowStock();
        this.categoryId = c != null ? c.getCategoryId() : null;
        this.categoryName = c != null ? c.getName() : null;
        this.description = p.getDescription();
        this.isActive = p.getIsActive();
        this.createdAt = p.getCreatedAt();
        this.updatedAt = p.getUpdatedAt();

    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(product);
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getBarcode() {
        return barcode;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
