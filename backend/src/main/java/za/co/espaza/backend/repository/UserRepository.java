package za.co.espaza.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.espaza.backend.entity.User;

import javax.management.relation.Role;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    List<User> findByIsActiveTrue();
    boolean existsByUsername(String username);
    long countByRoleAndIsActiveTrue(za.co.espaza.backend.Enum.Role role);
}
