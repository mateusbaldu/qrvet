package ipiranga.fatec.qrvet.dtos.request;

import jakarta.validation.constraints.*;

public record PaginationRequest(
        @NotNull @Min(0) Integer page,
        @NotNull @Min(1) @Max(100) Integer size) {
    public PaginationRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
    }
}
