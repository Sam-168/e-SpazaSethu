package za.co.espaza.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "category")
public class Category {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String categoryId;
    @Column(nullable = false, length = 100, unique = true)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        if(categoryId == null){
            categoryId = UUID.randomUUID().toString();
        }
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }

    public Category() {

    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
