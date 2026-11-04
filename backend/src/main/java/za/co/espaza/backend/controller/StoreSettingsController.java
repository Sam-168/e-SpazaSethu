package za.co.espaza.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.espaza.backend.dto.request.UpdateStoreSettingsRequest;
import za.co.espaza.backend.dto.response.StoreSettingsResponse;
import za.co.espaza.backend.service.StoreSettingsService;

@RestController
@RequestMapping("/api/v1/settings/store")
public class StoreSettingsController {
    private final StoreSettingsService service;

    public StoreSettingsController(StoreSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<StoreSettingsResponse> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreSettingsResponse> update(
            @Valid @RequestBody UpdateStoreSettingsRequest request) {
        return ResponseEntity.ok(service.update(request));
    }
}
