package za.co.espaza.backend.dto.response;

import za.co.espaza.backend.entity.Category;

import java.time.LocalDateTime;

public class CategoryResponse {
    private final String categoryId;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;

    public CategoryResponse(String categoryId, String name, String description, LocalDateTime createdAt) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt());
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
