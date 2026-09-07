package ipiranga.fatec.qrvet.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OperationConflictException extends RuntimeException {
    public OperationConflictException(String message) {
        super(message);
    }
}
