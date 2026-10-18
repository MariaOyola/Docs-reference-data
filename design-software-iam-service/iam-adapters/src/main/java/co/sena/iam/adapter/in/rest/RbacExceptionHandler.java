package co.sena.iam.adapter.in.rest;

import co.sena.iam.contract.model.Login400Response;
import co.sena.iam.domain.exception.RoleNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps RBAC catalog failures onto the shared error envelope. */
@RestControllerAdvice(assignableTypes = RbacController.class)
class RbacExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RbacExceptionHandler.class);

    @ExceptionHandler(RoleNotFoundException.class)
    ResponseEntity<Login400Response> handleRoleNotFound(
            RoleNotFoundException exception, HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Role not found: {} (trace {})", exception.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrors.of(
                IamErrorCode.NOT_FOUND,
                "El rol solicitado no existe.",
                traceId));
    }
}
