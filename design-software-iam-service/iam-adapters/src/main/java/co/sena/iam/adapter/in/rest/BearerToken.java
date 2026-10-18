package co.sena.iam.adapter.in.rest;

import co.sena.iam.domain.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;

/** Reads the bearer credential out of the Authorization header. */
final class BearerToken {

    private static final String PREFIX = "Bearer ";

    private BearerToken() {
    }

    static String from(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            throw new UnauthenticatedException("Missing Bearer authorization header");
        }
        String token = header.substring(PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthenticatedException("Empty bearer token");
        }
        return token;
    }
}
