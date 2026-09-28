package za.co.espaza.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.Enum.Role;
import za.co.espaza.backend.dto.request.CreateUserRequest;
import za.co.espaza.backend.dto.request.UpdateUserRequest;
import za.co.espaza.backend.dto.response.UserResponse;
import za.co.espaza.backend.entity.User;
import za.co.espaza.backend.exception.DuplicateResourceException;
import za.co.espaza.backend.exception.ResourceNotFoundException;
import za.co.espaza.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public UserResponse updateUser(String userId, UpdateUserRequest request) {

        User user = findUser(userId);

        if (Boolean.FALSE.equals(request.getIsActive()) && Boolean.TRUE.equals(user.getIsActive()) && user.getRole() == Role.ADMIN) {

            long activeAdminCount = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);

            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot deactivate the last remaining admin");
            }
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    public void resetPassword(String userId, String newPassword) {

        User user = findUser(userId);

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public void updateLastLogin(String userId) {

        User user = findUser(userId);

        user.setLastLogin(LocalDateTime.now());

        userRepository.save(user);
    }

    private User findUser(String userId) {

        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(user.getUserId(), user.getUsername(), user.getRole(), user.getIsActive(), user.getLastLogin(), user.getCreatedAt());
    }
}