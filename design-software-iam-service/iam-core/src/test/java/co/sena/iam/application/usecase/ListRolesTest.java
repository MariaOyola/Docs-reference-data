package co.sena.iam.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.sena.iam.domain.model.RbacRole;
import co.sena.iam.testsupport.InMemoryRbacCatalogRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListRolesTest {

    private static RbacRole role(String name) {
        return new RbacRole(UUID.randomUUID(), name, "Display " + name, null, true, Instant.EPOCH);
    }

    @Test
    @DisplayName("E1 returns all roles ordered by name")
    void returnsAllRoles() {
        List<RbacRole> roles = List.of(role("COORDINATOR"), role("INSTRUCTOR"), role("SYSTEM_ADMIN"));
        ListRoles useCase = new ListRoles(
                new InMemoryRbacCatalogRepository(List.of(), roles, null));

        List<RbacRole> result = useCase.listRoles();

        assertEquals(3, result.size());
        assertEquals("COORDINATOR", result.get(0).name());
        assertEquals("INSTRUCTOR", result.get(1).name());
        assertEquals("SYSTEM_ADMIN", result.get(2).name());
    }

    @Test
    @DisplayName("E2 system role flag is preserved")
    void systemRoleFlagPreserved() {
        RbacRole sysAdmin = role("SYSTEM_ADMIN");
        ListRoles useCase = new ListRoles(
                new InMemoryRbacCatalogRepository(List.of(), List.of(sysAdmin), null));

        assertTrue(useCase.listRoles().get(0).isSystemRole());
    }
}
