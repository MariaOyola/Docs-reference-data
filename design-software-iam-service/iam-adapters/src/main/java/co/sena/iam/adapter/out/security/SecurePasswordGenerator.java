package co.sena.iam.adapter.out.security;

import co.sena.iam.application.port.out.PasswordGenerator;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Generates a random 12-character temporary password with mixed character classes. */
@Component
class SecurePasswordGenerator implements PasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$!";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        char[] pw = new char[12];
        pw[0] = UPPER.charAt(random.nextInt(UPPER.length()));
        pw[1] = LOWER.charAt(random.nextInt(LOWER.length()));
        pw[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        pw[3] = SPECIAL.charAt(random.nextInt(SPECIAL.length()));
        for (int i = 4; i < 12; i++) {
            pw[i] = ALL.charAt(random.nextInt(ALL.length()));
        }
        for (int i = 11; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = pw[i]; pw[i] = pw[j]; pw[j] = tmp;
        }
        return new String(pw);
    }
}
