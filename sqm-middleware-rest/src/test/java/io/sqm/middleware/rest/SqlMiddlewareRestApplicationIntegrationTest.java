package io.sqm.middleware.rest;

import io.sqm.middleware.rest.adapter.*;
import io.sqm.middleware.rest.config.*;
import io.sqm.middleware.rest.controller.*;
import io.sqm.middleware.rest.error.*;
import io.sqm.middleware.rest.filter.*;
import io.sqm.middleware.rest.model.*;
import io.sqm.middleware.rest.ratelimit.*;
import io.sqm.middleware.rest.security.*;
import io.sqm.middleware.api.AnalyzeRequest;
import io.sqm.middleware.api.DecisionExplanationDto;
import io.sqm.middleware.api.DecisionKindDto;
import io.sqm.middleware.api.DecisionResultDto;
import io.sqm.middleware.api.ExecutionContextDto;
import io.sqm.middleware.api.ExecutionModeDto;
import io.sqm.middleware.api.EnforceRequest;
import io.sqm.middleware.api.ExplainRequest;
import io.sqm.middleware.api.ParameterizationModeDto;
import io.sqm.middleware.api.ReasonCodeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    classes = SqlMiddlewareRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class SqlMiddlewareRestApplicationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void analyze_endpoint_returns_decision_for_real_http_request() {
        var request = new AnalyzeRequest(
            "select id from users",
            new ExecutionContextDto("postgresql", "agent", "tenant-a", ExecutionModeDto.ANALYZE, ParameterizationModeDto.OFF)
        );

        var response = restTemplate.postForEntity(
            baseUrl() + "/sqm/middleware/v1/analyze",
            request,
            DecisionResultDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().kind());
        assertNotNull(response.getBody().reasonCode());
    }

    @Test
    void explain_endpoint_returns_explanation_for_real_http_request() {
        var request = new ExplainRequest(
            "select id from users",
            new ExecutionContextDto("postgresql", "agent", "tenant-a", ExecutionModeDto.ANALYZE, ParameterizationModeDto.OFF)
        );

        var response = restTemplate.postForEntity(
            baseUrl() + "/sqm/middleware/v1/explain",
            request,
            DecisionExplanationDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().decision());
        assertNotNull(response.getBody().explanation());
    }

    @Test
    void enforce_endpoint_denies_ddl_for_real_http_request() {
        var request = new EnforceRequest(
            "drop table users",
            new ExecutionContextDto("postgresql", "agent", "tenant-a", ExecutionModeDto.EXECUTE, ParameterizationModeDto.OFF)
        );

        var response = restTemplate.postForEntity(
            baseUrl() + "/sqm/middleware/v1/enforce",
            request,
            DecisionResultDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(DecisionKindDto.DENY, response.getBody().kind());
        assertTrue(
            response.getBody().reasonCode() == ReasonCodeDto.DENY_DDL
                || response.getBody().reasonCode() == ReasonCodeDto.DENY_VALIDATION
                || response.getBody().reasonCode() == ReasonCodeDto.DENY_PIPELINE_ERROR
        );
    }

    @Test
    void mysql_dml_requests_are_supported_over_real_http_endpoints() {
        var context = new ExecutionContextDto("mysql", "agent", "tenant-a", ExecutionModeDto.EXECUTE, ParameterizationModeDto.OFF);
        var analyze = restTemplate.postForEntity(
            baseUrl() + "/sqm/middleware/v1/analyze",
            new AnalyzeRequest("insert ignore into users (id, name) values (1, 'alice')", context),
            DecisionResultDto.class
        );
        var enforce = restTemplate.postForEntity(
            baseUrl() + "/sqm/middleware/v1/enforce",
            new EnforceRequest("update users set name = 'alice' where id = 1", context),
            DecisionResultDto.class
        );
        var explain = restTemplate.postForEntity(
            baseUrl() + "/sqm/middleware/v1/explain",
            new ExplainRequest("delete from users where id = 1", context),
            DecisionExplanationDto.class
        );

        assertEquals(HttpStatus.OK, analyze.getStatusCode());
        assertEquals(HttpStatus.OK, enforce.getStatusCode());
        assertEquals(HttpStatus.OK, explain.getStatusCode());
        assertNotNull(analyze.getBody());
        assertNotNull(enforce.getBody());
        assertNotNull(explain.getBody());
        assertNotNull(explain.getBody().decision());
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}

