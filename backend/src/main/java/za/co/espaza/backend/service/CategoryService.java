package za.co.espaza.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.dto.response.CategoryResponse;
import za.co.espaza.backend.dto.request.CreateCategoryRequest;
import za.co.espaza.backend.dto.request.UpdateCategoryRequest;
import za.co.espaza.backend.entity.Category;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.exception.DuplicateResourceException;
import za.co.espaza.backend.repository.CategoryRepository;
import za.co.espaza.backend.repository.ProductRepository;

import java.util.List;

public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new DuplicateResourceException(
                    "A category named '" + name + "' already exists");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(String id, UpdateCategoryRequest request) {
        Category category = findOrThrow(id);

        if (request.getName() != null) {
            String newName = request.getName().trim();
            boolean changed = !newName.equalsIgnoreCase(category.getName());
            if (changed) {
                boolean takenByAnother = categoryRepository.findByNameIgnoreCase(newName)
                        .filter(other -> !other.getCategoryId().equals(category.getCategoryId()))
                        .isPresent();
                if (takenByAnother) {
                    throw new DuplicateResourceException(
                            "A category named '" + newName + "' already exists");
                }
            }
            category.setName(newName);
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public void deleteCategory(String id) {
        Category category = findOrThrow(id);

        boolean hasProducts = !productRepository
                .findByIsActiveTrueAndCategory_CategoryId(category.getCategoryId())
                .isEmpty();
        if (hasProducts) {
            throw new BusinessRuleException("Cannot delete category with assigned products");
        }

        categoryRepository.delete(category);
    }

    private Category findOrThrow(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

}
