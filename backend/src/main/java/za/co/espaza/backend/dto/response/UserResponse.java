package za.co.espaza.backend.dto.response;

import za.co.espaza.backend.Enum.Role;

import java.time.LocalDateTime;

public class UserResponse {
    private final String userId;
    private final String username;
    private final Role role;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLogin;

    public UserResponse(String userId, String username, Role role, Boolean isActive, LocalDateTime createdAt, LocalDateTime lastLogin) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public Boolean getActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
}
