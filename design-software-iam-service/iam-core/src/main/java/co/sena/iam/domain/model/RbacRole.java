package co.sena.iam.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Role definition. Mirrors rbac.role. */
public record RbacRole(
        UUID id,
        String name,
        String displayName,
        String description,
        boolean isSystemRole,
        Instant createdAt) {

    public RbacRole {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
