package co.sena.iam.adapter.in.rest;

import co.sena.iam.contract.model.Login400Response;
import co.sena.iam.contract.model.Login400ResponseError;
import co.sena.iam.contract.model.Login400ResponseErrorDetailsInner;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

/**
 * Builds the shared error envelope {@code { error: { code, message, details, trace_id } }}.
 *
 * <p>The generated type is named after the first operation that returns it
 * ({@code Login400Response}); it is the {@code Error} schema of {@code _shared.yaml}.
 */
final class ApiErrors {

    private static final String TRACE_HEADER = "X-Request-Id";

    private ApiErrors() {
    }

    static Login400Response of(String code, String message, String traceId) {
        return new Login400Response().error(
                new Login400ResponseError().code(code).message(message).traceId(traceId));
    }

    static Login400Response of(
            String code, String message, String traceId, List<Login400ResponseErrorDetailsInner> details) {
        return new Login400Response().error(
                new Login400ResponseError().code(code).message(message).traceId(traceId).details(details));
    }

    static Login400ResponseErrorDetailsInner detail(String field, String issue) {
        return new Login400ResponseErrorDetailsInner().field(field).issue(issue);
    }

    /**
     * Correlation id for logs and audit-service. Uses the inbound request id when the gateway
     * sent one; HU-IAM-011 replaces this with the OpenTelemetry trace id.
     */
    static String traceId(HttpServletRequest request) {
        if (request != null) {
            String header = request.getHeader(TRACE_HEADER);
            if (header != null && !header.isBlank()) {
                return header;
            }
        }
        return UUID.randomUUID().toString();
    }
}
