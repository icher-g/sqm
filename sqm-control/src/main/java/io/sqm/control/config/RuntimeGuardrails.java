package io.sqm.control.config;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.service.SqlDecisionService;

import java.io.Serializable;

/**
 * Runtime guardrail settings for middleware execution.
 *
 * @param maxSqlLength  maximum SQL length in characters; {@code null} disables this guardrail
 * @param timeoutMillis evaluation timeout in milliseconds; {@code null} disables timeout
 * @param maxRows                 maximum allowed LIMIT value; {@code null} disables this guardrail
 * @param maxStatementsPerRequest maximum number of parsed statements allowed in one SQL request; {@code null} disables this guardrail
 * @param explainDryRun           when {@code true}, {@link SqlDecisionService#enforce(String, ExecutionContext)}
 *                                rewrites the effective SQL to {@code EXPLAIN <sql>} instead of changing analyze-mode behavior;
 *                                SQM does not execute the statement, it only returns the rewritten SQL for the caller to run
 */
public record RuntimeGuardrails(
    Integer maxSqlLength,
    Long timeoutMillis,
    Integer maxRows,
    Integer maxStatementsPerRequest,
    boolean explainDryRun
) implements Serializable {

    /**
     * Creates runtime guardrails using the legacy guardrail shape.
     *
     * @param maxSqlLength  maximum SQL length
     * @param timeoutMillis evaluation timeout in milliseconds
     * @param maxRows       maximum allowed LIMIT value
     * @param explainDryRun explain dry-run rewrite flag for execute-intent flow
     */
    public RuntimeGuardrails(Integer maxSqlLength, Long timeoutMillis, Integer maxRows, boolean explainDryRun) {
        this(maxSqlLength, timeoutMillis, maxRows, null, explainDryRun);
    }

    /**
     * Validates guardrail constraints.
     *
     * @param maxSqlLength           maximum SQL length
     * @param timeoutMillis          evaluation timeout in milliseconds
     * @param maxRows                maximum allowed LIMIT value
     * @param maxStatementsPerRequest maximum number of parsed statements allowed in one SQL request
     * @param explainDryRun          explain dry-run rewrite flag for execute-intent flow
     */
    public RuntimeGuardrails {
        if (maxSqlLength != null && maxSqlLength <= 0) {
            throw new IllegalArgumentException("maxSqlLength must be > 0");
        }
        if (timeoutMillis != null && timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be > 0");
        }
        if (maxRows != null && maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be > 0");
        }
        if (maxStatementsPerRequest != null && maxStatementsPerRequest <= 0) {
            throw new IllegalArgumentException("maxStatementsPerRequest must be > 0");
        }
    }

    /**
     * Returns disabled guardrails.
     *
     * @return disabled guardrail configuration
     */
    public static RuntimeGuardrails disabled() {
        return new RuntimeGuardrails(null, null, null, null, false);
    }
}





