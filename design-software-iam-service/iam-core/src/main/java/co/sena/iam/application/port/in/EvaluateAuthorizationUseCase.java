package co.sena.iam.application.port.in;

import co.sena.iam.domain.model.AuthorizationDecision;

/**
 * HU-IAM-006 — evaluates whether a user may exercise a feature within a target scope.
 * HU-IAM-010 consumes this to stamp the decision as a JWT claim for local verification.
 */
public interface EvaluateAuthorizationUseCase {

    AuthorizationDecision evaluate(AuthorizationQuery query);
}
