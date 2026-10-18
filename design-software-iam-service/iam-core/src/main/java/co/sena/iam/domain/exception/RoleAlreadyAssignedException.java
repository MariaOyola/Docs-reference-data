package co.sena.iam.domain.exception;

public final class RoleAlreadyAssignedException extends RuntimeException {

    public RoleAlreadyAssignedException(String roleName) {
        super("Role already assigned: " + roleName);
    }
}
