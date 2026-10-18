package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.sena.iam.domain.exception.RoleNotFoundException;
import co.sena.iam.domain.model.RoleFeatureEntry;
import co.sena.iam.testsupport.InMemoryRbacCatalogRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListRoleFeaturesTest {

    private static final UUID ROLE_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID UNKNOWN_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");

    private static RoleFeatureEntry featureEntry(String code, String scopeType) {
        return new RoleFeatureEntry(code, "Feat " + code, "READ", scopeType);
    }

    @Test
    @DisplayName("E1/E3 returns features with scope_type for a known role")
    void returnsFeaturesForKnownRole() {
        List<RoleFeatureEntry> entries = List.of(
                featureEntry("IDENTITY_USER_VIEW", "GLOBAL"),
                featureEntry("IDENTITY_ROLE_VIEW", "GLOBAL"));
        ListRoleFeatures useCase = new ListRoleFeatures(
                new InMemoryRbacCatalogRepository(List.of(), List.of(), Map.of(ROLE_ID, entries)));

        List<RoleFeatureEntry> result = useCase.listRoleFeatures(ROLE_ID);

        assertEquals(2, result.size());
        assertEquals("IDENTITY_USER_VIEW", result.get(0).featureCode());
        assertEquals("GLOBAL", result.get(0).scopeType());
    }

    @Test
    @DisplayName("E2 scope_type TRAINING_CENTER differentiates a center-scoped role from global")
    void centerScopedFeatureHasTrainingCenterScope() {
        List<RoleFeatureEntry> entries = List.of(
                featureEntry("IDENTITY_USER_VIEW", "TRAINING_CENTER"));
        ListRoleFeatures useCase = new ListRoleFeatures(
                new InMemoryRbacCatalogRepository(List.of(), List.of(), Map.of(ROLE_ID, entries)));

        assertEquals("TRAINING_CENTER", useCase.listRoleFeatures(ROLE_ID).get(0).scopeType());
    }

    @Test
    @DisplayName("E1 unknown role id throws RoleNotFoundException → 404")
    void unknownRoleThrows() {
        ListRoleFeatures useCase = new ListRoleFeatures(
                new InMemoryRbacCatalogRepository(List.of(), List.of(), Map.of()));

        assertThrows(RoleNotFoundException.class, () -> useCase.listRoleFeatures(UNKNOWN_ID));
    }
}
