package ipiranga.fatec.qrvet.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
            .body(new ApiError(exception.getMessage(), exception.getCode(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.unprocessableEntity()
            .body(new ApiError("Verifique os dados informados.", "VALIDATION_ERROR", fields));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiError("Você não tem permissão para esta ação.", "FORBIDDEN", null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ApiError("A imagem deve ter no máximo 5 MB.", "AVATAR_TOO_LARGE", null));
    }

    public record ApiError(String message, String code, Map<String, String> fieldErrors) {}
}
