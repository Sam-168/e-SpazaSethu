package za.co.espaza.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, String> details
) {
    // Convenience constructor for errors without per-field details
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, LocalDateTime.now(), null);
    }

    // Convenience constructor for errors with per-field details
    public ErrorResponse(int status, String error, String message, String path, Map<String, String> details) {
        this(status, error, message, path, LocalDateTime.now(), details);
    }
}

