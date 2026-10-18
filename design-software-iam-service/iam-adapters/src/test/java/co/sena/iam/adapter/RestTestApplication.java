package co.sena.iam.adapter;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point for the MVC slice tests of this module. The adapters module is a library,
 * so the web tests need a {@code @SpringBootConfiguration} of their own; the real deployable
 * lives in {@code iam-api}.
 */
@SpringBootApplication
public class RestTestApplication {
}
