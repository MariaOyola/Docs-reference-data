package co.sena.iam.application.port.in;

/** Inbound port — HU-IAM-001 E1/E2/E3: authenticate, audit, and enforce RN-IAM-01. */
public interface AuthenticateUserUseCase {

    /**
     * @throws co.sena.iam.domain.exception.AccountLockedException     account is serving a lockout
     * @throws co.sena.iam.domain.exception.InvalidCredentialsException unknown email, wrong
     *                                                                 password, or disabled account
     */
    AuthenticationResult authenticate(LoginCommand command);
}
