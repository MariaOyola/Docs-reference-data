package co.sena.iam.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Points JPA at the persistence adapter, which lives in a sibling module and therefore
 * outside the packages Boot scans by default.
 *
 * <p>Gated on a configured datasource so the MVC slice tests — which run with no database at
 * all — do not drag an entity manager into their context.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.datasource.url")
@EntityScan(basePackages = "co.sena.iam.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "co.sena.iam.adapter.out.persistence")
class PersistenceConfiguration {
}
