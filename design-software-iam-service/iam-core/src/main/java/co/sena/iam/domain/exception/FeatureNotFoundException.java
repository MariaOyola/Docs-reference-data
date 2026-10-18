package co.sena.iam.domain.exception;

/** A scope override referenced a feature code that is not in {@code rbac_catalog.feature}. */
public final class FeatureNotFoundException extends RuntimeException {

    public FeatureNotFoundException(String featureCode) {
        super("Feature not found: " + featureCode);
    }
}
