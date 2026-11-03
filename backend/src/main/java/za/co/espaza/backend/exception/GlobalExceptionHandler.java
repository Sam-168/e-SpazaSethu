package za.co.espaza.backend.exception;

import za.co.espaza.backend.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Validation failures (@Valid on request bodies) ───────────────────────
    // When a field fails validation (e.g. blank name, negative price),
    // Spring throws this. We collect ALL field errors into one message
    // and provide per-field details so the frontend knows exactly what's wrong.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.merge(fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                    (existing, msg) -> existing + "; " + msg);
        }

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, request.getRequestURI(), details));
    }

    // ── Malformed JSON or an unparseable value (e.g. unknown enum constant) ───
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

        Map<String, String> details = Map.of(ex.getParameterName(), "parameter is required");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request",
                        ex.getParameterName() + ": parameter is required",
                        request.getRequestURI(), details));
    }

    // ── Type mismatch on query parameters or path variables ───────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String field = ex.getName();
        String message = field + ": Invalid value";
        Map<String, String> details = Map.of(field, "Invalid value");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, request.getRequestURI(), details));
    }

    // ── Constraint violations from validated parameters ──────────────────────
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> details = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "value";
            details.merge(field, violation.getMessage(), (existing, m) -> existing + "; " + m);
        }

        String message = details.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message, request.getRequestURI(), details));
    }

    // ── Method-level validation (Spring 6.1+ HandlerMethodValidator) ──────────
    // Raised when constraints fail directly on a controller method parameter,
    // e.g. @Valid on a List<DTO> request body (element-level validation).
    // Without this handler the catch-all below would return 500.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {

        Map<String, String> details = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = extractFieldName(result, error);
                String msg = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
                details.merge(field, msg, (existing, m) -> existing + "; " + m);
            }
        }

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
                        message, request.getRequestURI(), details.isEmpty() ? null : details));
    }

    private String extractFieldName(ParameterValidationResult result, MessageSourceResolvable error) {
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
        return field;
    }

    // Renders one violation as "field: message", unwrapping the underlying
    // Bean Validation ConstraintViolation to recover the field name. Falls back
    // to the parameter name when the source is not a constraint violation.
    private String toFieldMessage(ParameterValidationResult result, MessageSourceResolvable error) {
        String field = extractFieldName(result, error);
        return field + ": " + error.getDefaultMessage();
    }

    // ── Illegal argument exceptions ──────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    // ── HTTP method not allowed ──────────────────────────────────────────────
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(405, "Method Not Allowed", ex.getMessage(), request.getRequestURI()));
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

        log.error("Unhandled exception processing request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error",
                        "Something went wrong on our end. Please try again.",
                        request.getRequestURI()));
    }
}
