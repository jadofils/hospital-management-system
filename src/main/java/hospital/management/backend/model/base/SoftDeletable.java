package hospital.management.backend.model.base;

import java.time.LocalDateTime;

public interface SoftDeletable {
    LocalDateTime getDeletedAt();

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }

    void markDeleted();
}