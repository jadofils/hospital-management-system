package hospital.management.backend.model.base;

import java.time.LocalDateTime;

public interface Auditable {
    LocalDateTime getCreatedAt();
}