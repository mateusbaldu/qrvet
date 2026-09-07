package ipiranga.fatec.qrvet.exceptions;

import ipiranga.fatec.qrvet.dtos.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> errorResponse(
            HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status)
                .body(
                        new ErrorResponse(
                                status.name(),
                                message,
                                fields,
                                Instant.now(),
                                UUID.randomUUID().toString()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> resourceNotFound(ResourceNotFoundException e) {
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage(), Map.of());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> resourceAlreadyExists(ResourceAlreadyExistsException e) {
        return errorResponse(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ErrorResponse> invalidRequest(InvalidRequestException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), Map.of());
    }

    @ExceptionHandler(OperationConflictException.class)
    ResponseEntity<ErrorResponse> operationConflict(OperationConflictException e) {
        return errorResponse(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }
}
