package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.User;
import java.util.List;
import java.util.Objects;

public record UserPage(List<User> items, long totalItems, int page, int size) {

    public UserPage {
        Objects.requireNonNull(items, "items");
    }

    public int totalPages() {
        if (size == 0) return 0;
        return (int) Math.ceil((double) totalItems / size);
    }
}
