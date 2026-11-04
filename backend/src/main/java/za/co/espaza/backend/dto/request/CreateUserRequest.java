package za.co.espaza.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import za.co.espaza.backend.Enum.Role;

public class CreateUserRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 50)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8 , message = "Password must contain at least 8 characters")
    private String password;

    private Role role;

    public CreateUserRequest() {}

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }
}
