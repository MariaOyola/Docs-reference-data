package co.sena.iam.domain.exception;

public final class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String roleName) {
        super("Role not found: " + roleName);
    }

    public RoleNotFoundException(java.util.UUID roleId) {
        super("Role not found: " + roleId);
    }
}
