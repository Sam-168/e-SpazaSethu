package za.co.espaza.backend.exception;

import za.co.espaza.backend.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, request.getRequestURI()));
    }

    // ── Malformed JSON or an unparseable value (e.g. unknown enum constant) ───
    // The request body could not even be turned into a DTO, so this is a client
    // error. Without this handler the catch-all below would return a 500.
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBodyException(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request",
                        "Request body is malformed or contains an invalid value",
                        request.getRequestURI()));
    }

    // ── Missing required query parameter (e.g. /reports/summary without from) ─
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request",
                        ex.getParameterName() + ": parameter is required",
                        request.getRequestURI()));
    }

    // ── Method-level validation (Spring 6.1+ HandlerMethodValidator) ──────────
    // Raised when constraints fail directly on a controller method parameter,
    // e.g. @Valid on a List<DTO> request body (element-level validation).
    // Without this handler the catch-all below would return 500.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {

        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> toFieldMessage(result, error)))
                .collect(Collectors.joining(", "));
        if (message.isEmpty()) {
            message = "Validation failure";
        }

        int status = ex.getStatusCode().value();
        return ResponseEntity
                .status(HttpStatus.valueOf(status))
                .body(new ErrorResponse(status,
                        status == 400 ? "Bad Request" : "Internal Server Error",
                        message, request.getRequestURI()));
    }

    // Renders one violation as "field: message", unwrapping the underlying
    // Bean Validation ConstraintViolation to recover the field name. Falls back
    // to the parameter name when the source is not a constraint violation.
    private String toFieldMessage(ParameterValidationResult result, MessageSourceResolvable error) {
        String field = null;
        try {
            ConstraintViolation<?> violation = result.unwrap(error, ConstraintViolation.class);
            field = violation.getPropertyPath().toString();
        } catch (Exception ignored) {
            // Source is not a Bean Validation violation — use the parameter name below.
        }
        if (field == null || field.isEmpty()) {
            String parameterName = result.getMethodParameter().getParameterName();
            field = parameterName != null ? parameterName : "value";
        }
        return field + ": " + error.getDefaultMessage();
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
