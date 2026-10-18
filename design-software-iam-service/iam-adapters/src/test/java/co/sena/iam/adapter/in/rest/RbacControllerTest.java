package co.sena.iam.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.ListModulesUseCase;
import co.sena.iam.application.port.in.ListRoleFeaturesUseCase;
import co.sena.iam.application.port.in.ListRolesUseCase;
import co.sena.iam.domain.exception.FeatureNotFoundException;
import co.sena.iam.domain.exception.RoleNotFoundException;
import co.sena.iam.domain.exception.ScopeOverrideNotFoundException;
import co.sena.iam.domain.exception.UnauthenticatedException;
import co.sena.iam.domain.model.ActorType;
import co.sena.iam.domain.model.LockoutState;
import co.sena.iam.domain.model.RbacFeature;
import co.sena.iam.domain.model.RbacModule;
import co.sena.iam.domain.model.RbacRole;
import co.sena.iam.domain.model.RoleFeatureEntry;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import co.sena.iam.domain.model.User;
import co.sena.iam.domain.model.UserAccess;
import co.sena.iam.application.port.in.AuthenticatedIdentity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP contract of HU-IAM-004: status codes, payload shape, and auth guard. */
@WebMvcTest(controllers = RbacController.class)
class RbacControllerTest {

    private static final UUID USER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID MODULE_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID ROLE_ID = UUID.fromString("e0000000-0000-0000-0000-000000000002");
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER = "Bearer " + VALID_TOKEN;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetCurrentUserUseCase getCurrentUser;

    @MockBean
    private ListModulesUseCase listModulesUseCase;

    @MockBean
    private ListRolesUseCase listRolesUseCase;

    @MockBean
    private ListRoleFeaturesUseCase listRoleFeaturesUseCase;

    @MockBean
    private UserApplicationService userService;

    @MockBean
    private ScopeOverrideApplicationService scopeOverrideService;

    private static AuthenticatedIdentity authenticatedIdentity() {
        User user = new User(USER_ID, "admin@sena.edu.co", "hash", "Admin", "Test",
                ActorType.USER, null, true, LockoutState.clear(), false, null);
        UserAccess access = new UserAccess(List.of(), List.of("IDENTITY_ROLE_VIEW"), List.of("MOD_IDENTITY"));
        return new AuthenticatedIdentity(user, access);
    }

    @BeforeEach
    void stubAuth() {
        given(getCurrentUser.currentUser(VALID_TOKEN)).willReturn(authenticatedIdentity());
    }

    // ---------------------------------------------------------------- GET /modules

    @Test
    @DisplayName("E1 GET /modules returns module list with nested features")
    void listModulesReturnsModules() throws Exception {
        RbacFeature feature = new RbacFeature("IDENTITY_USER_VIEW", "Ver usuarios", null, "READ", "GLOBAL");
        RbacModule module = new RbacModule(MODULE_ID, "MOD_IDENTITY", "Identidad", null, 1, null, List.of(feature));
        given(listModulesUseCase.listModules()).willReturn(List.of(module));

        mvc.perform(get("/api/v1/modules").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MOD_IDENTITY"))
                .andExpect(jsonPath("$[0].name").value("Identidad"))
                .andExpect(jsonPath("$[0].features[0].code").value("IDENTITY_USER_VIEW"))
                .andExpect(jsonPath("$[0].features[0].action_level").value("READ"));
    }

    @Test
    @DisplayName("E1 GET /modules without token answers 401")
    void listModulesWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/modules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("E1 GET /modules with invalid token answers 401")
    void listModulesWithInvalidTokenReturns401() throws Exception {
        willThrow(new UnauthenticatedException("invalid"))
                .given(getCurrentUser).currentUser("bad");

        mvc.perform(get("/api/v1/modules").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    // ---------------------------------------------------------------- GET /roles

    @Test
    @DisplayName("E1 GET /roles returns role list")
    void listRolesReturnsRoles() throws Exception {
        RbacRole role = new RbacRole(ROLE_ID, "SYSTEM_ADMIN", "Administrador de Sistema", null, true, Instant.EPOCH);
        given(listRolesUseCase.listRoles()).willReturn(List.of(role));

        mvc.perform(get("/api/v1/roles").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ROLE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("SYSTEM_ADMIN"))
                .andExpect(jsonPath("$[0].display_name").value("Administrador de Sistema"))
                .andExpect(jsonPath("$[0].is_system_role").value(true));
    }

    @Test
    @DisplayName("E2 GET /roles returns role without training_center_id — global role")
    void globalRoleHasNoTrainingCenter() throws Exception {
        RbacRole role = new RbacRole(ROLE_ID, "SYSTEM_ADMIN", "Administrador", null, true, Instant.EPOCH);
        given(listRolesUseCase.listRoles()).willReturn(List.of(role));

        mvc.perform(get("/api/v1/roles").header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("SYSTEM_ADMIN"));
    }

    @Test
    @DisplayName("E1 GET /roles without token answers 401")
    void listRolesWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- GET /roles/{id}/features

    @Test
    @DisplayName("E1/E3 GET /roles/{id}/features returns features with scope_type")
    void listRoleFeaturesReturnsFeatures() throws Exception {
        RoleFeatureEntry entry = new RoleFeatureEntry("IDENTITY_USER_VIEW", "Ver usuarios", "READ", "GLOBAL");
        given(listRoleFeaturesUseCase.listRoleFeatures(ROLE_ID)).willReturn(List.of(entry));

        mvc.perform(get("/api/v1/roles/{id}/features", ROLE_ID).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].feature_code").value("IDENTITY_USER_VIEW"))
                .andExpect(jsonPath("$[0].feature_name").value("Ver usuarios"))
                .andExpect(jsonPath("$[0].action_level").value("READ"))
                .andExpect(jsonPath("$[0].scope_type").value("GLOBAL"));
    }

    @Test
    @DisplayName("E2 TRAINING_CENTER scope_type is preserved in the response")
    void centerScopedFeaturePreservesScopeType() throws Exception {
        RoleFeatureEntry entry = new RoleFeatureEntry("IDENTITY_USER_MANAGE", "Gestionar usuarios", "WRITE", "TRAINING_CENTER");
        given(listRoleFeaturesUseCase.listRoleFeatures(ROLE_ID)).willReturn(List.of(entry));

        mvc.perform(get("/api/v1/roles/{id}/features", ROLE_ID).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scope_type").value("TRAINING_CENTER"));
    }

    @Test
    @DisplayName("E1 unknown role id answers 404")
    void unknownRoleIdReturns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        willThrow(new RoleNotFoundException(unknownId))
                .given(listRoleFeaturesUseCase).listRoleFeatures(unknownId);

        mvc.perform(get("/api/v1/roles/{id}/features", unknownId).header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("E1 GET /roles/{id}/features without token answers 401")
    void listRoleFeaturesWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/roles/{id}/features", ROLE_ID))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- HU-IAM-006 E3 — scope overrides

    private static ScopeOverrideRecord sampleOverride() {
        return new ScopeOverrideRecord(
                UUID.fromString("f0000000-0000-0000-0000-000000000009"),
                "SCH_CREATE", ScopeType.GLOBAL, true, "temporary coverage",
                USER_ID, null, OffsetDateTime.parse("2026-08-26T12:00:00Z"));
    }

    @Test
    @DisplayName("E3 POST /users/{id}/scope-overrides returns 201 with the created override")
    void createScopeOverrideReturns201() throws Exception {
        given(scopeOverrideService.grant(any())).willReturn(sampleOverride());

        mvc.perform(post("/api/v1/users/{id}/scope-overrides", USER_ID)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feature_code\":\"SCH_CREATE\",\"scope_type\":\"GLOBAL\","
                                + "\"is_allowed\":true,\"reason\":\"temporary coverage\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feature_code").value("SCH_CREATE"))
                .andExpect(jsonPath("$.scope_type").value("GLOBAL"))
                .andExpect(jsonPath("$.is_allowed").value(true))
                .andExpect(jsonPath("$.granted_by").value(USER_ID.toString()));
    }

    @Test
    @DisplayName("E3 POST with an unknown feature answers 422")
    void createScopeOverrideUnknownFeatureReturns422() throws Exception {
        willThrow(new FeatureNotFoundException("DOES_NOT_EXIST"))
                .given(scopeOverrideService).grant(any());

        mvc.perform(post("/api/v1/users/{id}/scope-overrides", USER_ID)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feature_code\":\"DOES_NOT_EXIST\",\"scope_type\":\"GLOBAL\","
                                + "\"is_allowed\":true,\"reason\":\"x\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("IAM_FEATURE_NOT_FOUND"));
    }

    @Test
    @DisplayName("E3 GET /users/{id}/scope-overrides returns the active overrides")
    void listScopeOverridesReturnsList() throws Exception {
        given(scopeOverrideService.list(USER_ID)).willReturn(List.of(sampleOverride()));

        mvc.perform(get("/api/v1/users/{id}/scope-overrides", USER_ID).header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].feature_code").value("SCH_CREATE"))
                .andExpect(jsonPath("$[0].is_allowed").value(true));
    }

    @Test
    @DisplayName("E3 DELETE /users/{id}/scope-overrides/{override_id} returns 204")
    void revokeScopeOverrideReturns204() throws Exception {
        mvc.perform(delete("/api/v1/users/{id}/scope-overrides/{overrideId}", USER_ID, UUID.randomUUID())
                        .header("Authorization", BEARER))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("E3 DELETE with an unknown override answers 404")
    void revokeUnknownScopeOverrideReturns404() throws Exception {
        UUID overrideId = UUID.randomUUID();
        willThrow(new ScopeOverrideNotFoundException(overrideId))
                .given(scopeOverrideService).revoke(any(), any());

        mvc.perform(delete("/api/v1/users/{id}/scope-overrides/{overrideId}", USER_ID, overrideId)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("IAM_SCOPE_OVERRIDE_NOT_FOUND"));
    }

    @Test
    @DisplayName("E3 POST /users/{id}/scope-overrides without token answers 401")
    void createScopeOverrideWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/api/v1/users/{id}/scope-overrides", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feature_code\":\"SCH_CREATE\",\"scope_type\":\"GLOBAL\","
                                + "\"is_allowed\":true,\"reason\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
