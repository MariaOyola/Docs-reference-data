package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.sena.iam.domain.model.RbacFeature;
import co.sena.iam.domain.model.RbacModule;
import co.sena.iam.testsupport.InMemoryRbacCatalogRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListModulesTest {

    private static final UUID MOD_ID = UUID.randomUUID();

    private static RbacModule module(String code, RbacFeature... features) {
        return new RbacModule(MOD_ID, code, "Display " + code, null, 1, null, List.of(features));
    }

    private static RbacFeature feature(String code) {
        return new RbacFeature(code, "Feat " + code, null, "READ", "GLOBAL");
    }

    @Test
    @DisplayName("E1 returns modules with their features in catalog order")
    void returnsModulesWithFeatures() {
        RbacModule mod = module("MOD_IDENTITY", feature("IDENTITY_USER_VIEW"), feature("IDENTITY_ROLE_VIEW"));
        InMemoryRbacCatalogRepository repo = new InMemoryRbacCatalogRepository(List.of(mod), List.of(), null);
        ListModules useCase = new ListModules(repo);

        List<RbacModule> result = useCase.listModules();

        assertEquals(1, result.size());
        assertEquals("MOD_IDENTITY", result.get(0).code());
        assertEquals(2, result.get(0).features().size());
        assertEquals("IDENTITY_USER_VIEW", result.get(0).features().get(0).code());
    }

    @Test
    @DisplayName("E1 returns empty list when no modules exist")
    void returnsEmptyWhenNoModules() {
        ListModules useCase = new ListModules(
                new InMemoryRbacCatalogRepository(List.of(), List.of(), null));
        assertTrue(useCase.listModules().isEmpty());
    }
}
