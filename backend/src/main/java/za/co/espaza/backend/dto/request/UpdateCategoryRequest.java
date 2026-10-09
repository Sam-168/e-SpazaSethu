package za.co.espaza.backend.dto.request;

import jakarta.validation.constraints.Pattern;

public class UpdateCategoryRequest {
    // Partial update: null means "leave unchanged". @NotBlank would also reject
    // null (making name mandatory), so @Pattern is used instead — it passes for
    // null but rejects empty/whitespace-only names.
    @Pattern(regexp = ".*\\S.*", message = "Category name must not be blank")
    private String name;
    private String description;

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
}
