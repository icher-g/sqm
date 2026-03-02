package io.sqm.middleware.rest;

import io.sqm.control.ConfigKeys;
import io.sqm.middleware.api.SqlMiddlewareService;
import io.sqm.middleware.core.SqlMiddlewareRuntime;
import io.sqm.middleware.core.SqlMiddlewareRuntimeFactory;
import io.sqm.middleware.rest.adapter.SqlMiddlewareRestAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * Standalone REST application entry point for SQL middleware.
 */
@SpringBootApplication
public class SqlMiddlewareRestApplication {

    /**
     * Creates the REST application configuration.
     */
    public SqlMiddlewareRestApplication() {
    }

    /**
     * Launches the REST application.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SqlMiddlewareRestApplication.class, args);
    }

    /**
     * Provides default middleware runtime wiring for standalone host.
     *
     * <p>Configuration precedence:
     * JVM system properties, then environment variables, then Spring property sources
     * (for example {@code application.properties}) as a fallback.</p>
     *
     * @param environment Spring environment
     * @return middleware runtime with diagnostics
     */
    @Bean
    public SqlMiddlewareRuntime sqlMiddlewareRuntime(Environment environment) {
        applySpringFallbackProperties(environment);
        return SqlMiddlewareRuntimeFactory.createRuntimeFromEnvironment();
    }

    /**
     * Provides middleware service from runtime container.
     *
     * @param runtime middleware runtime container
     * @return middleware service
     */
    @Bean
    public SqlMiddlewareService sqlMiddlewareService(SqlMiddlewareRuntime runtime) {
        return runtime.service();
    }

    /**
     * Provides REST adapter bean.
     *
     * @param service middleware service
     * @return REST adapter
     */
    @Bean
    public SqlMiddlewareRestAdapter sqlMiddlewareRestAdapter(SqlMiddlewareService service) {
        return new SqlMiddlewareRestAdapter(service);
    }

    static void applySpringFallbackProperties(Environment environment) {
        Objects.requireNonNull(environment, "environment must not be null");
        for (Field field : ConfigKeys.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != ConfigKeys.Key.class) {
                continue;
            }
            try {
                var key = (ConfigKeys.Key) field.get(null);
                applySpringFallbackProperty(environment, key);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to access configuration key field: " + field.getName(), e);
            }
        }
        applySpringFallbackProperty(environment, ConfigKeys.Key.of("spring.profiles.active", "SPRING_PROFILES_ACTIVE"));
    }

    static void applySpringFallbackProperty(Environment environment, ConfigKeys.Key key) {
        Objects.requireNonNull(environment, "environment must not be null");
        Objects.requireNonNull(key, "key must not be null");
        if (isExplicitlyConfigured(key)) {
            return;
        }
        var fallback = environment.getProperty(key.property());
        if (fallback != null && !fallback.isBlank()) {
            System.setProperty(key.property(), fallback);
        }
    }

    private static boolean isExplicitlyConfigured(ConfigKeys.Key key) {
        var propertyValue = System.getProperty(key.property());
        if (propertyValue != null && !propertyValue.isBlank()) {
            return true;
        }
        var envValue = System.getenv(key.env());
        return envValue != null && !envValue.isBlank();
    }
}


