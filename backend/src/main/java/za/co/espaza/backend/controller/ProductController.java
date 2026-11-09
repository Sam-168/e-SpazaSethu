package za.co.espaza.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import za.co.espaza.backend.dto.request.CreateProductRequest;
import za.co.espaza.backend.dto.response.ProductResponse;
import za.co.espaza.backend.dto.request.UpdateProductRequest;
import za.co.espaza.backend.service.ProductService;
import za.co.espaza.backend.service.BarcodeLookupService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;
    private final BarcodeLookupService barcodeLookupService;

    public ProductController(ProductService productService, BarcodeLookupService barcodeLookupService) {
        this.productService = productService;
        this.barcodeLookupService = barcodeLookupService;
    }

    @GetMapping("/barcode-lookup/{barcode}")
    @PreAuthorize("hasRole('ADMIN')")
    public BarcodeLookupService.LookupResult lookupBarcode(@PathVariable String barcode) {
        return barcodeLookupService.lookup(barcode);
    }

    @GetMapping
   @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) String categoryId) {
        return ResponseEntity.ok(productService.searchProducts(search, categoryId));
    }

    @GetMapping("/low-stock")
   @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) String categoryId) {
        return ResponseEntity.ok(productService.searchAllProducts(search, categoryId));
    }

    @GetMapping("/{id}")
   @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable("id") String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable("id") String id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize ("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateProduct(@PathVariable("id") String id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> reactivateProduct(@PathVariable("id") String id) {
        return ResponseEntity.ok(productService.reactivateProduct(id));
    }


}
