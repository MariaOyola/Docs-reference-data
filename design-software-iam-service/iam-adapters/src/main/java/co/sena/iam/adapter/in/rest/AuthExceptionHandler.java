package co.sena.iam.adapter.in.rest;

import co.sena.iam.contract.model.Login400Response;
import co.sena.iam.contract.model.Login400ResponseErrorDetailsInner;
import co.sena.iam.domain.exception.AccountLockedException;
import co.sena.iam.domain.exception.InvalidCredentialsException;
import co.sena.iam.domain.exception.InvalidRefreshTokenException;
import co.sena.iam.domain.exception.InvalidResetTokenException;
import co.sena.iam.domain.exception.PasswordChangeRequiredException;
import co.sena.iam.domain.exception.RateLimitExceededException;
import co.sena.iam.domain.exception.SessionNotFoundException;
import co.sena.iam.domain.exception.TemporaryPasswordExpiredException;
import co.sena.iam.domain.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps authentication and session failures onto the shared error envelope.
 */
@RestControllerAdvice(assignableTypes = {AuthController.class, SessionController.class})
class AuthExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(AccountLockedException.class)
    ResponseEntity<Login400Response> handleAccountLocked(
            AccountLockedException exception, HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Login refused: account locked until {} (trace {})", exception.lockedUntil(), traceId);
        return ResponseEntity.status(HttpStatus.LOCKED).body(ApiErrors.of(
                IamErrorCode.ACCOUNT_LOCKED,
                "La cuenta está bloqueada por intentos fallidos. Intenta más tarde.",
                traceId));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Login400Response> handleInvalidCredentials(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Login refused: invalid credentials (trace {})", traceId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrors.of(
                IamErrorCode.INVALID_CREDENTIALS,
                "Correo o contraseña incorrectos.",
                traceId));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<Login400Response> handleInvalidRefreshToken(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Refresh refused: token invalid, expired or revoked (trace {})", traceId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrors.of(
                IamErrorCode.TOKEN_REVOKED,
                "El refresh token es inválido, expiró o fue revocado.",
                traceId));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<Login400Response> handleUnauthenticated(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrors.of(
                IamErrorCode.UNAUTHENTICATED,
                "Token ausente, inválido o expirado.",
                traceId));
    }

    @ExceptionHandler(SessionNotFoundException.class)
    ResponseEntity<Login400Response> handleSessionNotFound(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrors.of(
                IamErrorCode.SESSION_NOT_FOUND,
                "Sesión no encontrada.",
                traceId));
    }

    @ExceptionHandler(PasswordChangeRequiredException.class)
    ResponseEntity<Login400Response> handlePasswordChangeRequired(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Login refused: must change temporary password (trace {})", traceId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiErrors.of(
                IamErrorCode.PASSWORD_CHANGE_REQUIRED,
                "Debes cambiar tu contraseña temporal antes de continuar.",
                traceId));
    }

    @ExceptionHandler(TemporaryPasswordExpiredException.class)
    ResponseEntity<Login400Response> handleTemporaryPasswordExpired(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Login refused: temporary password expired (trace {})", traceId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrors.of(
                IamErrorCode.TEMPORARY_PASSWORD_EXPIRED,
                "Tu contraseña temporal ha expirado. Solicita un nuevo reset.",
                traceId));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<Login400Response> handleRateLimitExceeded(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Password reset rate-limited (trace {})", traceId);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiErrors.of(
                IamErrorCode.RATE_LIMIT_EXCEEDED,
                "Demasiadas solicitudes. Intenta más tarde.",
                traceId));
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    ResponseEntity<Login400Response> handleInvalidResetToken(HttpServletRequest request) {
        String traceId = ApiErrors.traceId(request);
        log.info("Password reset confirm refused: invalid token (trace {})", traceId);
        return ResponseEntity.badRequest().body(ApiErrors.of(
                IamErrorCode.INVALID_RESET_TOKEN,
                "El token de reset es inválido, ya fue usado o expiró.",
                traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Login400Response> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Login400ResponseErrorDetailsInner> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> ApiErrors.detail(error.getField(), issueCodeOf(error.getCode())))
                .toList();
        return ResponseEntity.badRequest().body(ApiErrors.of(
                IamErrorCode.VALIDATION_ERROR,
                "El payload no es válido.",
                ApiErrors.traceId(request),
                details));
    }

    private static String issueCodeOf(String constraintName) {
        if (constraintName == null) {
            return "INVALID";
        }
        return constraintName.replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_").toUpperCase(java.util.Locale.ROOT);
    }
}
