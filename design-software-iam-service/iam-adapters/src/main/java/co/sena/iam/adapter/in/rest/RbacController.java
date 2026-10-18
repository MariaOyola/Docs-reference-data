package co.sena.iam.adapter.in.rest;

import co.sena.iam.application.port.in.AssignRoleCommand;
import co.sena.iam.application.port.in.AuthenticatedIdentity;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.GrantScopeOverrideCommand;
import co.sena.iam.application.port.in.ListModulesUseCase;
import co.sena.iam.application.port.in.ListRoleFeaturesUseCase;
import co.sena.iam.application.port.in.ListRolesUseCase;
import co.sena.iam.application.port.in.UserRoleResult;
import co.sena.iam.contract.api.ModulesApi;
import co.sena.iam.contract.api.RolesApi;
import co.sena.iam.contract.model.AssignRoleRequest;
import co.sena.iam.contract.model.Feature;
import co.sena.iam.contract.model.Module;
import co.sena.iam.contract.model.Role;
import co.sena.iam.contract.model.RoleFeature;
import co.sena.iam.contract.model.ScopeOverride;
import co.sena.iam.contract.model.ScopeOverrideCreate;
import co.sena.iam.contract.model.UserRole;
import co.sena.iam.domain.model.RbacModule;
import co.sena.iam.domain.model.RbacRole;
import co.sena.iam.domain.model.RoleFeatureEntry;
import co.sena.iam.domain.model.ScopeOverrideRecord;
import co.sena.iam.domain.model.ScopeType;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * HU-IAM-004 E1–E3 — RBAC catalog read endpoints.
 *
 * <p>Authentication: Bearer token validated by GetCurrentUserUseCase. Feature-level
 * authorization (IDENTITY_ROLE_VIEW) is enforced by HU-IAM-006.
 */
@RestController
@RequestMapping("/api/v1")
class RbacController implements ModulesApi, RolesApi {

    private final GetCurrentUserUseCase getCurrentUser;
    private final ListModulesUseCase listModules;
    private final ListRolesUseCase listRoles;
    private final ListRoleFeaturesUseCase listRoleFeatures;
    private final UserApplicationService userService;
    private final ScopeOverrideApplicationService scopeOverrideService;
    private final HttpServletRequest request;

    RbacController(
            GetCurrentUserUseCase getCurrentUser,
            ListModulesUseCase listModules,
            ListRolesUseCase listRoles,
            ListRoleFeaturesUseCase listRoleFeatures,
            UserApplicationService userService,
            ScopeOverrideApplicationService scopeOverrideService,
            HttpServletRequest request) {
        this.getCurrentUser = getCurrentUser;
        this.listModules = listModules;
        this.listRoles = listRoles;
        this.listRoleFeatures = listRoleFeatures;
        this.userService = userService;
        this.scopeOverrideService = scopeOverrideService;
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    // ---------------------------------------------------------------- GET /modules

    @Override
    public ResponseEntity<List<Module>> listModules() {
        requireAuthenticated();
        List<Module> body = listModules.listModules().stream()
                .map(RbacController::toModule)
                .toList();
        return ResponseEntity.ok(body);
    }

    // ---------------------------------------------------------------- GET /roles

    @Override
    public ResponseEntity<List<Role>> listRoles() {
        requireAuthenticated();
        List<Role> body = listRoles.listRoles().stream()
                .map(RbacController::toRole)
                .toList();
        return ResponseEntity.ok(body);
    }

    // ---------------------------------------------------------------- GET /roles/{id}/features

    @Override
    public ResponseEntity<List<RoleFeature>> listRoleFeatures(UUID id) {
        requireAuthenticated();
        List<RoleFeature> body = listRoleFeatures.listRoleFeatures(id).stream()
                .map(RbacController::toRoleFeature)
                .toList();
        return ResponseEntity.ok(body);
    }

    // ---------------------------------------------------------------- POST /users/{id}/roles (HU-IAM-005 E4)

    @Override
    public ResponseEntity<UserRole> assignUserRole(UUID id, AssignRoleRequest body) {
        AuthenticatedIdentity caller = requireAuthenticated();
        var cmd = new AssignRoleCommand(
                id, caller.user().id(), body.getRoleName(),
                body.getTrainingCenterId(),
                body.getExpiresAt());
        UserRoleResult result = userService.assignRole(cmd);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(toUserRole(result));
    }

    // ---------------------------------------------------------------- DELETE /users/{id}/roles/{role_name}

    @Override
    public ResponseEntity<Void> revokeUserRole(UUID id, String roleName) {
        requireAuthenticated();
        userService.revokeRole(id, roleName, null);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- HU-IAM-006 E3 — scope overrides

    // GET /users/{id}/scope-overrides
    @Override
    public ResponseEntity<List<ScopeOverride>> listScopeOverrides(UUID id) {
        requireAuthenticated();
        List<ScopeOverride> body = scopeOverrideService.list(id).stream()
                .map(RbacController::toScopeOverride)
                .toList();
        return ResponseEntity.ok(body);
    }

    // POST /users/{id}/scope-overrides
    @Override
    public ResponseEntity<ScopeOverride> createScopeOverride(UUID id, ScopeOverrideCreate body) {
        AuthenticatedIdentity caller = requireAuthenticated();
        var cmd = new GrantScopeOverrideCommand(
                id,
                body.getFeatureCode(),
                ScopeType.fromCode(body.getScopeType().getValue()),
                body.getIsAllowed(),
                body.getReason(),
                caller.user().id(),
                body.getExpiresAt());
        ScopeOverrideRecord created = scopeOverrideService.grant(cmd);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(toScopeOverride(created));
    }

    // DELETE /users/{id}/scope-overrides/{override_id}
    @Override
    public ResponseEntity<Void> revokeScopeOverride(UUID id, UUID overrideId) {
        requireAuthenticated();
        scopeOverrideService.revoke(id, overrideId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- auth guard

    /** Validates the Bearer token and returns the caller; throws UnauthenticatedException on failure. */
    private AuthenticatedIdentity requireAuthenticated() {
        return getCurrentUser.currentUser(BearerToken.from(request));
    }

    // ---------------------------------------------------------------- mappers

    private static Module toModule(RbacModule m) {
        return new Module()
                .code(m.code())
                .name(m.name())
                .description(m.description())
                .displayOrder(m.displayOrder())
                .iconKey(m.iconKey())
                .features(m.features().stream().map(RbacController::toFeature).toList());
    }

    private static Feature toFeature(co.sena.iam.domain.model.RbacFeature f) {
        return new Feature()
                .code(f.code())
                .name(f.name())
                .description(f.description())
                .actionLevel(Feature.ActionLevelEnum.fromValue(f.actionLevel()))
                .scopeType(Feature.ScopeTypeEnum.fromValue(f.scopeType()));
    }

    private static Role toRole(RbacRole r) {
        return new Role()
                .id(r.id())
                .name(r.name())
                .displayName(r.displayName())
                .description(r.description())
                .isSystemRole(r.isSystemRole())
                .createdAt(OffsetDateTime.ofInstant(r.createdAt(), ZoneOffset.UTC));
    }

    private static RoleFeature toRoleFeature(RoleFeatureEntry e) {
        return new RoleFeature()
                .featureCode(e.featureCode())
                .featureName(e.featureName())
                .actionLevel(RoleFeature.ActionLevelEnum.fromValue(e.actionLevel()))
                .scopeType(RoleFeature.ScopeTypeEnum.fromValue(e.scopeType()));
    }

    private static ScopeOverride toScopeOverride(ScopeOverrideRecord r) {
        return new ScopeOverride()
                .id(r.id())
                .featureCode(r.featureCode())
                .scopeType(ScopeOverride.ScopeTypeEnum.fromValue(r.scopeType().name()))
                .isAllowed(r.allowed())
                .reason(r.reason())
                .grantedBy(r.grantedBy())
                .expiresAt(r.expiresAt())
                .createdAt(r.createdAt());
    }

    private static UserRole toUserRole(UserRoleResult r) {
        return new UserRole()
                .id(r.id())
                .userId(r.userId())
                .roleId(r.roleId())
                .roleName(r.roleName())
                .trainingCenterId(r.trainingCenterId())
                .assignedBy(r.assignedBy())
                .assignedAt(r.assignedAt())
                .expiresAt(r.expiresAt());
    }
}
