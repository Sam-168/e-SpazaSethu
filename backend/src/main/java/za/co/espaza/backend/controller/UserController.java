package za.co.espaza.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import za.co.espaza.backend.dto.request.CreateUserRequest;
import za.co.espaza.backend.dto.request.ResetPasswordRequest;
import za.co.espaza.backend.dto.request.UpdateUserRequest;
import za.co.espaza.backend.dto.response.UserResponse;
import za.co.espaza.backend.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
   @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        return ResponseEntity.ok(userService.getAllUsers()
        );
    }


    @PostMapping
   @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") String userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        UserResponse response =
                userService.updateUser(userId, request);

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(
            @PathVariable("id") String userId,
            @Valid @RequestBody ResetPasswordRequest request
    ) {

        userService.resetPassword(
                userId,
                request.getNewPassword()
        );

        return ResponseEntity.noContent().build();
    }
}
