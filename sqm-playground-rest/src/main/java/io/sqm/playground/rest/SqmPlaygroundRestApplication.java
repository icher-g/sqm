package io.sqm.playground.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone REST application entry point for the SQM playground host.
 */
@SpringBootApplication
public class SqmPlaygroundRestApplication {

    /**
     * Creates the REST application configuration.
     */
    public SqmPlaygroundRestApplication() {
    }

    /**
     * Launches the REST application.
     *
     * @param args process arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SqmPlaygroundRestApplication.class, args);
    }
}
