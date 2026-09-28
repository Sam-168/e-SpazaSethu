package za.co.espaza.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.dto.request.CreateProductRequest;
import za.co.espaza.backend.dto.response.ProductResponse;
import za.co.espaza.backend.dto.request.UpdateProductRequest;
import za.co.espaza.backend.entity.Category;
import za.co.espaza.backend.entity.Product;
import za.co.espaza.backend.exception.DuplicateResourceException;
import za.co.espaza.backend.repository.CategoryRepository;
import za.co.espaza.backend.repository.ProductRepository;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String barcode = normalizeBarcode(request.getBarcode());
        if (barcode != null && productRepository.findByBarcode(barcode).isPresent()) {
            throw new DuplicateResourceException(
                    "A product with barcode '" + barcode + "' already exists");
        }

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setBarcode(barcode);
        product.setSellingPrice(request.getSellingPrice());
        product.setCostPrice(request.getCostPrice());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setDescription(request.getDescription());
        product.setCategory(resolveCategory(request.getCategoryId()));
        product.setIsActive(true);

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(String id, UpdateProductRequest request) {
        Product product = findOrThrow(id);

        if (request.getBarcode() != null) {
            String newBarcode = normalizeBarcode(request.getBarcode());
            boolean changed = newBarcode != null && !newBarcode.equals(product.getBarcode());
            if (changed) {
                boolean takenByAnother = productRepository.findByBarcode(newBarcode)
                        .filter(other -> !other.getProductId().equals(product.getProductId()))
                        .isPresent();
                if (takenByAnother) {
                    throw new DuplicateResourceException(
                            "A product with barcode '" + newBarcode + "' already exists");
                }
            }
            product.setBarcode(newBarcode);
        }
        if (request.getName() != null) {
            product.setName(request.getName().trim());
        }
        if (request.getSellingPrice() != null) {
            product.setSellingPrice(request.getSellingPrice());
        }
        if (request.getCostPrice() != null) {
            product.setCostPrice(request.getCostPrice());
        }
        if (request.getLowStockThreshold() != null) {
            product.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            product.setCategory(resolveCategory(request.getCategoryId()));
        }

        // @PreUpdate on Product refreshes updatedAt on flush.
        return ProductResponse.from(productRepository.save(product));
    }

    /** Soft delete: the row stays, isActive becomes false. */
    @Transactional
    public void deactivateProduct(String id) {
        Product product = findOrThrow(id);
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String search, String categoryId) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = categoryId != null && !categoryId.isBlank();

        List<Product> products;
        if (hasSearch && hasCategory) {
            products = productRepository
                    .findByIsActiveTrueAndNameContainingIgnoreCaseAndCategory_CategoryId(
                            search.trim(), categoryId);
        } else if (hasSearch) {
            products = productRepository.findByIsActiveTrueAndNameContainingIgnoreCase(search.trim());
        } else if (hasCategory) {
            products = productRepository.findByIsActiveTrueAndCategory_CategoryId(categoryId);
        } else {
            products = productRepository.findByIsActiveTrue();
        }
        return products.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }

    // ---- helpers ----

    private Product findOrThrow(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
    }

    private Category resolveCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found with id: " + categoryId));
    }

    /** Blank barcodes are stored as NULL so they don't collide with each other. */
    private String normalizeBarcode(String barcode) {
        return (barcode == null || barcode.isBlank()) ? null : barcode.trim();
    }


}
