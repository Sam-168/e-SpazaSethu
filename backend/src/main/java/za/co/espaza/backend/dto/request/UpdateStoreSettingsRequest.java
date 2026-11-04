package za.co.espaza.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStoreSettingsRequest(
        @NotBlank(message = "Shop name is required")
        @Size(max = 120, message = "Shop name must be 120 characters or fewer")
        String shopName,
        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a three-letter code")
        String currencyCode
) { }
