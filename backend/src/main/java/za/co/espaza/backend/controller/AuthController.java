package za.co.espaza.backend.controller;
import jakarta.validation.Valid;
import za.co.espaza.backend.dto.request.LoginRequest;
import za.co.espaza.backend.dto.request.SetupOwnerRequest;
import za.co.espaza.backend.dto.response.ErrorResponse;
import za.co.espaza.backend.dto.response.LoginResponse;
import za.co.espaza.backend.dto.response.SetupStatusResponse;
import za.co.espaza.backend.dto.response.UserResponse;
import za.co.espaza.backend.security.CustomUserDetailsService;
import za.co.espaza.backend.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import za.co.espaza.backend.security.UserPrincipal;
import za.co.espaza.backend.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // This line does two things: checks the username exists,
            // and verifies the password matches the BCrypt hash in the DB
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse(401, "Unauthorized", "Incorrect username or password", "/api/v1/auth/login")
            );
        }

        // If we get here, credentials are valid — generate a token
        UserPrincipal userDetails = (UserPrincipal) userDetailsService.loadUserByUsername(request.username());
        String token = jwtUtil.generateToken(userDetails, userDetails.getUserId());
        userService.updateLastLogin(userDetails.getUserId().toString());

        String authority = userDetails.getAuthorities().iterator().next().getAuthority();
        LoginResponse response = new LoginResponse(
                token,
                new LoginResponse.UserInfo(
                        userDetails.getUserId().toString(),
                        userDetails.getUsername(),
                        authority.replaceFirst("^ROLE_", ""))
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/setup-status")
    public SetupStatusResponse getSetupStatus() {
        return new SetupStatusResponse(userService.isInitialSetupRequired());
    }

    @PostMapping("/setup-owner")
    public ResponseEntity<UserResponse> setupOwner(@Valid @RequestBody SetupOwnerRequest request) {
        UserResponse owner = userService.createInitialOwner(request.username(), request.password());
        return ResponseEntity.status(201).body(owner);
    }
}
