package co.sena.iam.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read model for a catalog module with its active features.
 * Mirrors rbac_catalog.module joined with rbac_catalog.feature.
 */
public record RbacModule(
        UUID id,
        String code,
        String name,
        String description,
        int displayOrder,
        String iconKey,
        List<RbacFeature> features) {

    public RbacModule {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        features = List.copyOf(Objects.requireNonNull(features, "features"));
    }
}
