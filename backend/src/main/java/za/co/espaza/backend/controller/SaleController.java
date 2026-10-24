package za.co.espaza.backend.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import za.co.espaza.backend.dto.request.CreateSaleRequest;
import za.co.espaza.backend.security.CurrentUserService;
import za.co.espaza.backend.dto.response.SaleResponse;
import za.co.espaza.backend.services.SaleService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleService saleService;
    private final CurrentUserService currentUserService;

    public SaleController(SaleService saleService, CurrentUserService currentUserService) {
        this.saleService = saleService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody CreateSaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleService.createSale(request, currentUserService.getCurrentUserId()));
    }

    // GET /api/v1/sales?from=2026-01-01&to=2026-01-31  (all roles)
    // Defaults to today when the dates are omitted.
    @GetMapping
    public ResponseEntity<List<SaleResponse>> getSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate today = LocalDate.now();
        LocalDate start = (from != null) ? from : today;
        LocalDate end = (to != null) ? to : today;

        return ResponseEntity.ok(saleService.getSales(start, end));
    }

    // GET /api/v1/sales/{id}  (all roles)
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSaleById(@PathVariable UUID id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    // PATCH /api/v1/sales/{id}/cancel  (admin only)
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SaleResponse> cancelSale(@PathVariable UUID id) {
        return ResponseEntity.ok(saleService.cancelSale(id, currentUserService.getCurrentUserId()));
    }
}
