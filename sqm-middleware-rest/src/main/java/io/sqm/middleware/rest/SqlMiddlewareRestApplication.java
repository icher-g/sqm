package io.sqm.middleware.rest;

import io.sqm.middleware.api.SqlMiddlewareService;
import io.sqm.middleware.core.SqlMiddlewareRuntimeFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
     * Provides default middleware service wiring for standalone runtime.
     *
     * @return middleware service
     */
    @Bean
    public SqlMiddlewareService sqlMiddlewareService() {
        return SqlMiddlewareRuntimeFactory.createFromEnvironment();
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
}
