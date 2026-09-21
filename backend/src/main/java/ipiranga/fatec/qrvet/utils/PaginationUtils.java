package ipiranga.fatec.qrvet.utils;

import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {
    private PaginationUtils() {}

    public static PageRequest byId(PaginationRequest pagination) {
        return byId(pagination, Sort.Direction.DESC);
    }

    public static PageRequest byId(PaginationRequest pagination, Sort.Direction direction) {
        return PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(direction, "id"));
    }

    public static PageRequest byId(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }
}
