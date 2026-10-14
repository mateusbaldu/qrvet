package ipiranga.fatec.qrvet.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ipiranga.fatec.qrvet.dtos.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        logger.warn("Validation failed for request: {}", e.getMessage());
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return errorResponse(HttpStatus.BAD_REQUEST, "Confira os campos informados.", fields);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> uploadTooLarge() {
        logger.warn("Request rejected because the uploaded file exceeded the configured size");
        return errorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Escolha uma foto de até 5 MB.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> duplicate(DataIntegrityViolationException e) {
        logger.warn("Request violated a data integrity constraint: {}", e.getMessage());
        return errorResponse(HttpStatus.CONFLICT, "Um cadastro com esses dados já existe. Atualize a página e confira os dados.", Map.of());
    }
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
        logger.warn("Resource not found: {}", e.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage(), Map.of());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    ResponseEntity<ErrorResponse> resourceAlreadyExists(ResourceAlreadyExistsException e) {
        logger.warn("Resource already exists: {}", e.getMessage());
        return errorResponse(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ErrorResponse> invalidRequest(InvalidRequestException e) {
        logger.warn("Invalid application request: {}", e.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), Map.of());
    }

    @ExceptionHandler(OperationConflictException.class)
    ResponseEntity<ErrorResponse> operationConflict(OperationConflictException e) {
        logger.warn("Application operation conflict: {}", e.getMessage());
        return errorResponse(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException e) {
        logger.warn("Access denied for request: {}", e.getMessage());
        return errorResponse(HttpStatus.FORBIDDEN, "Acesso não permitido.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        logger.error("Unexpected application error: {}", e.getMessage(), e);
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.",
                Map.of());
    }
}
