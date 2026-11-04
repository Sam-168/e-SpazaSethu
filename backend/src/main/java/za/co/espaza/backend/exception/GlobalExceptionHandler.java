package za.co.espaza.backend.exception;

import za.co.espaza.backend.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", "A valid JSON request body is required",
                        request.getRequestURI()));
    }

    // ── Validation failures (@Valid on request bodies) ───────────────────────
    // When a field fails validation (e.g. blank name, negative price),
    // Spring throws this. We collect ALL field errors into one message
    // so the frontend knows exactly what's wrong.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, request.getRequestURI()));
    }

    // ── Entity not found ─────────────────────────────────────────────────────
    // Thrown when someone requests a product, sale, or user that doesn't exist.
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({jakarta.persistence.EntityNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleOtherNotFoundExceptions(
            RuntimeException ex,
            HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    // ── Business rule violations ──────────────────────────────────────────────
    // Thrown when valid data breaks a business rule, e.g. stock going below zero.
    // 422 Unprocessable Entity is more accurate than 400 here because the
    // request itself is valid — the business logic rejected it.
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(
            BusinessRuleException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, "Unprocessable Entity", ex.getMessage(), request.getRequestURI()));
    }

    // ── Duplicate resource ────────────────────────────────────────────────────
    // Thrown when something must be unique but isn't, e.g. duplicate barcode.
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    // ── Access denied ─────────────────────────────────────────────────────────
    // Thrown by Spring Security when a cashier hits an admin-only endpoint.
    // IMPORTANT: This must be declared before the catch-all Exception handler
    // or Spring will catch AccessDeniedException as a generic Exception first.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden",
                        "You do not have permission to perform this action",
                        request.getRequestURI()));
    }

    // ── Authentication failure ────────────────────────────────────────────────
    // Thrown when a request has no token or an invalid token.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized",
                        "Authentication required",
                        request.getRequestURI()));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────
    // Catches anything we didn't anticipate. We deliberately hide the real
    // exception message because it could expose internal implementation details.
    // Check your server logs for the full stack trace instead.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        // Log it so you can see it in the console even though we hide it from the client
        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error",
                        "Something went wrong on our end. Please try again.",
                        request.getRequestURI()));
    }
}
