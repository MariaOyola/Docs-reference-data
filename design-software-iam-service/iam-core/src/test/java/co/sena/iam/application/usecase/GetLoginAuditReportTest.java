package co.sena.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.sena.iam.application.port.in.AuthorizationQuery;
import co.sena.iam.application.port.in.LoginAuditPage;
import co.sena.iam.application.port.in.LoginAuditQuery;
import co.sena.iam.application.port.out.LoginAuditQueryPort;
import co.sena.iam.domain.exception.ForbiddenException;
import co.sena.iam.domain.model.AuthorizationDecision;
import co.sena.iam.domain.model.DecisionReason;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-IAM-009 E1-E2. */
class GetLoginAuditReportTest {

    private static final UUID CALLER_ID = UUID.fromString("c0000000-0000-0000-0000-000000000009");
    private static final UUID CENTER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("E2 a caller without the AUDIT_LOG_VIEW grant is refused before the query port is touched")
    void deniesWithoutGrant() {
        RecordingQueryPort port = new RecordingQueryPort(emptyPage());
        GetLoginAuditReport useCase = new GetLoginAuditReport(
                query -> AuthorizationDecision.deny(DecisionReason.DENIED_NO_GRANT), port);

        assertThatThrownBy(() -> useCase.getReport(query()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(port.invoked).isFalse();
    }

    @Test
    @DisplayName("E2 an active block override refuses even a role-granted caller")
    void deniesOnBlockOverride() {
        RecordingQueryPort port = new RecordingQueryPort(emptyPage());
        GetLoginAuditReport useCase = new GetLoginAuditReport(
                query -> AuthorizationDecision.deny(DecisionReason.DENIED_BY_OVERRIDE), port);

        assertThatThrownBy(() -> useCase.getReport(query()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(port.invoked).isFalse();
    }

    @Test
    @DisplayName("E1 a permitted caller gets the page the query port returns")
    void delegatesToQueryPortWhenPermitted() {
        LoginAuditPage page = emptyPage();
        RecordingQueryPort port = new RecordingQueryPort(page);
        GetLoginAuditReport useCase = new GetLoginAuditReport(
                query -> AuthorizationDecision.permit(DecisionReason.PERMITTED_BY_ROLE), port);

        LoginAuditPage result = useCase.getReport(query());

        assertThat(result).isSameAs(page);
        assertThat(port.invoked).isTrue();
    }

    @Test
    @DisplayName("evaluates AUDIT_LOG_VIEW against the caller's own primary training center")
    void evaluatesRequestedFeatureAndTargetCenter() {
        AuthorizationQuery[] captured = new AuthorizationQuery[1];
        RecordingQueryPort port = new RecordingQueryPort(emptyPage());
        GetLoginAuditReport useCase = new GetLoginAuditReport(query -> {
            captured[0] = query;
            return AuthorizationDecision.permit(DecisionReason.PERMITTED_BY_ROLE);
        }, port);

        useCase.getReport(query());

        assertThat(captured[0].userId()).isEqualTo(CALLER_ID);
        assertThat(captured[0].featureCode()).isEqualTo("AUDIT_LOG_VIEW");
        assertThat(captured[0].trainingCenterId()).isEqualTo(CENTER_ID);
    }

    private static LoginAuditQuery query() {
        return new LoginAuditQuery(CALLER_ID, CENTER_ID, null, null, null, null, null, null, 50);
    }

    private static LoginAuditPage emptyPage() {
        return new LoginAuditPage(List.of(), null, 50, false);
    }

    private static final class RecordingQueryPort implements LoginAuditQueryPort {
        private final LoginAuditPage toReturn;
        private boolean invoked;

        RecordingQueryPort(LoginAuditPage toReturn) {
            this.toReturn = toReturn;
        }

        @Override
        public LoginAuditPage query(LoginAuditQuery query) {
            invoked = true;
            return toReturn;
        }
    }
}
