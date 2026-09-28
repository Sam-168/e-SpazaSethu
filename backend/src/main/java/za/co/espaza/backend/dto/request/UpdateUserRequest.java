package za.co.espaza.backend.dto.request;

import za.co.espaza.backend.Enum.Role;

public class UpdateUserRequest {
    private Role role;
    private Boolean isActive;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}
