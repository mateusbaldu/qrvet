package ipiranga.fatec.qrvet.exceptions;

import ipiranga.fatec.qrvet.dtos.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(org.springframework.web.bind.MethodArgumentNotValidException e) {
        var fields = new java.util.LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return errorResponse(HttpStatus.BAD_REQUEST, "Confira os campos informados.", fields);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> uploadTooLarge() {
        return errorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Escolha uma foto de até 5 MB.", Map.of());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> duplicate() {
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
