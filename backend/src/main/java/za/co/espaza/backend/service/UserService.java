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
import za.co.espaza.backend.exception.BusinessRuleException;
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

    @Transactional
    public synchronized UserResponse createInitialOwner(String username, String password) {
        if (userRepository.count() > 0) {
            throw new DuplicateResourceException("Owner setup is already complete");
        }

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(Role.ADMIN);
        return createUser(request);
    }

    @Transactional(readOnly = true)
    public boolean isInitialSetupRequired() {
        return userRepository.count() == 0;
    }

    @Transactional
    public UserResponse updateUser(String userId, String currentUserId, UpdateUserRequest request) {

        User user = findUser(userId);

        boolean roleChanges = request.getRole() != null && request.getRole() != user.getRole();
        boolean statusChanges = request.getIsActive() != null && !request.getIsActive().equals(user.getIsActive());

        if (userId.equals(currentUserId) && (roleChanges || statusChanges)) {
            throw new BusinessRuleException("You cannot change the role or status of your own account");
        }

        boolean removesActiveAdmin = user.getRole() == Role.ADMIN
                && Boolean.TRUE.equals(user.getIsActive())
                && (request.getRole() == Role.CASHIER || Boolean.FALSE.equals(request.getIsActive()));

        if (removesActiveAdmin) {

            long activeAdminCount = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);

            if (activeAdminCount <= 1) {
                throw new BusinessRuleException("You cannot remove or deactivate the last active administrator");
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

    @Transactional
    public void changeOwnPassword(String userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessRuleException("New password must be different from the current password");
        }
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

        return new UserResponse(user.getUserId(), user.getUsername(), user.getRole(), user.getIsActive(), user.getCreatedAt(), user.getLastLogin());
    }
}
