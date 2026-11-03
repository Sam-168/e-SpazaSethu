package za.co.espaza.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import za.co.espaza.backend.dto.request.StocktakeCountRequest;
import za.co.espaza.backend.dto.response.StocktakeResponse;
import za.co.espaza.backend.security.CurrentUserService;
import za.co.espaza.backend.services.StocktakeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stocktakes")
@PreAuthorize("hasRole('ADMIN')")
public class StocktakeController {

    private final StocktakeService stocktakeService;
    private final CurrentUserService currentUserService;

    public StocktakeController(StocktakeService stocktakeService,
                               CurrentUserService currentUserService) {
        this.stocktakeService = stocktakeService;
        this.currentUserService = currentUserService;
    }

    // POST /api/v1/stocktakes - start a new stocktake (admin)
    @PostMapping
    public ResponseEntity<StocktakeResponse> startStocktake() {
        UUID userId = currentUserService.getCurrentUserId();
        StocktakeResponse response = stocktakeService.startStocktake(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/stocktakes - list the current user's stocktakes, newest first (admin)
    @GetMapping
    public ResponseEntity<List<StocktakeResponse>> getStocktakes() {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(stocktakeService.getStocktakesForUser(userId));
    }

    // GET /api/v1/stocktakes/{id} - full detail with all items (admin)
    @GetMapping("/{id}")
    public ResponseEntity<StocktakeResponse> getStocktakeById(@PathVariable UUID id) {
        return ResponseEntity.ok(stocktakeService.getStocktakeById(id));
    }

    // POST /api/v1/stocktakes/{id}/items - submit counted quantities (admin)
    @PostMapping("/{id}/items")
    public ResponseEntity<StocktakeResponse> submitCounts(
            @PathVariable UUID id,
            @Valid @RequestBody List<StocktakeCountRequest> counts) {
        return ResponseEntity.ok(stocktakeService.submitCounts(id, counts));
    }

    // POST /api/v1/stocktakes/{id}/apply - apply adjustments and complete (admin)
    @PostMapping("/{id}/apply")
    public ResponseEntity<StocktakeResponse> applyAdjustments(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(stocktakeService.applyAdjustments(id, userId));
    }
}
