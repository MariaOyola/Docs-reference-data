package co.sena.iam.adapter.in.rest;

import co.sena.iam.contract.model.Login400Response;
import co.sena.iam.domain.exception.ForbiddenException;
import co.sena.iam.domain.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps login-audit report failures (HU-IAM-009) onto the shared error envelope. */
@RestControllerAdvice(assignableTypes = ReportController.class)
class ReportExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ReportExceptionHandler.class);

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<Login400Response> handleUnauthenticated(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrors.of(
                IamErrorCode.UNAUTHENTICATED,
                "Token ausente, inválido o expirado.",
                traceId));
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<Login400Response> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Login-audit report refused: {} (trace {})", exception.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiErrors.of(
                IamErrorCode.FORBIDDEN,
                "No tienes el feature requerido para consultar este reporte.",
                traceId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Login400Response> handleInvalidQuery(
            IllegalArgumentException exception, HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        return ResponseEntity.badRequest().body(ApiErrors.of(
                IamErrorCode.VALIDATION_ERROR,
                exception.getMessage(),
                traceId));
    }
}
