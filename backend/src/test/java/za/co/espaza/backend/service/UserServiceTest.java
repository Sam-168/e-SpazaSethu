package za.co.espaza.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.espaza.backend.Enum.Role;
import za.co.espaza.backend.dto.request.UpdateUserRequest;
import za.co.espaza.backend.entity.User;
import za.co.espaza.backend.exception.BusinessRuleException;
import za.co.espaza.backend.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void administratorCannotChangeTheirOwnAccess() {
        User admin = user("admin-1", Role.ADMIN, true);
        UpdateUserRequest request = update(Role.CASHIER, true);
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.updateUser("admin-1", "admin-1", request));

        assertEquals("You cannot change the role or status of your own account", error.getMessage());
        verify(userRepository, never()).save(admin);
    }

    @Test
    void lastActiveAdministratorCannotBeDemoted() {
        User admin = user("admin-1", Role.ADMIN, true);
        UpdateUserRequest request = update(Role.CASHIER, true);
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(Role.ADMIN)).thenReturn(1L);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.updateUser("admin-1", "admin-2", request));

        assertEquals("You cannot remove or deactivate the last active administrator", error.getMessage());
    }

    @Test
    void anotherAdministratorCanDeactivateACashier() {
        User cashier = user("cashier-1", Role.CASHIER, true);
        UpdateUserRequest request = update(Role.CASHIER, false);
        when(userRepository.findById("cashier-1")).thenReturn(Optional.of(cashier));
        when(userRepository.save(cashier)).thenReturn(cashier);

        var response = service.updateUser("cashier-1", "admin-1", request);

        assertEquals(false, response.getIsActive());
        verify(userRepository).save(cashier);
    }

    @Test
    void accountOwnerMustProvideTheCurrentPassword() {
        User user = user("cashier-1", Role.CASHIER, true);
        user.setPasswordHash("stored-hash");
        when(userRepository.findById("cashier-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.changeOwnPassword("cashier-1", "wrong-password", "NewPassword123"));

        assertEquals("Current password is incorrect", error.getMessage());
        verify(userRepository, never()).save(user);
    }

    @Test
    void accountOwnerCanChangeTheirPassword() {
        User user = user("cashier-1", Role.CASHIER, true);
        user.setPasswordHash("stored-hash");
        when(userRepository.findById("cashier-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "stored-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "stored-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.changeOwnPassword("cashier-1", "old-password", "new-password");

        assertEquals("new-hash", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    private User user(String id, Role role, boolean active) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(id);
        user.setRole(role);
        user.setIsActive(active);
        return user;
    }

    private UpdateUserRequest update(Role role, boolean active) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole(role);
        request.setIsActive(active);
        return request;
    }
}
