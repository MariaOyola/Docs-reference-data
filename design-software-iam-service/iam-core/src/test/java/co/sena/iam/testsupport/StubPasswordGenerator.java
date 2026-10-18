package co.sena.iam.testsupport;

import co.sena.iam.application.port.out.PasswordGenerator;

public final class StubPasswordGenerator implements PasswordGenerator {

    public static final String GENERATED = "Temp@Pass1!";

    @Override
    public String generate() {
        return GENERATED;
    }
}
