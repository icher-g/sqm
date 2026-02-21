package io.sqm.control;

/**
 * Default guidance mapping for reason codes.
 */
public final class ReasonGuidanceCatalog {
    private ReasonGuidanceCatalog() {
    }

    /**
     * Returns default guidance for a reason code.
     *
     * @param reasonCode reason code
     * @return mapped guidance or {@code null} if no default guidance is defined
     */
    public static DecisionGuidance forReason(ReasonCode reasonCode) {
        if (reasonCode == null) {
            throw new IllegalArgumentException("reasonCode must not be null");
        }
        return switch (reasonCode) {
            case DENY_DDL -> DecisionGuidance.retryable(
                "DDL statements are blocked in this context.",
                "remove_ddl",
                "Rewrite as a read-only SELECT query and do not use CREATE/ALTER/DROP."
            );
            case DENY_DML -> DecisionGuidance.retryable(
                "DML statements are blocked in this context.",
                "remove_dml",
                "Rewrite as a read-only SELECT query and do not use INSERT/UPDATE/DELETE."
            );
            case DENY_TABLE -> DecisionGuidance.retryable(
                "One or more referenced tables are not allowed.",
                "use_allowed_tables",
                "Use only tables explicitly allowed by policy."
            );
            case DENY_COLUMN -> DecisionGuidance.retryable(
                "One or more referenced columns are not allowed.",
                "remove_restricted_columns",
                "Remove denied columns from SELECT, WHERE, GROUP BY, and ORDER BY."
            );
            case DENY_FUNCTION -> DecisionGuidance.retryable(
                "One or more functions are not allowed.",
                "replace_denied_functions",
                "Replace denied functions with policy-approved alternatives."
            );
            case DENY_MAX_JOINS -> DecisionGuidance.retryable(
                "The query exceeds the maximum allowed number of joins.",
                "reduce_joins",
                "Reduce the number of joins and request only essential tables."
            );
            case DENY_MAX_SELECT_COLUMNS -> DecisionGuidance.retryable(
                "The query projects too many columns.",
                "reduce_projection",
                "Select only required columns and avoid SELECT *."
            );
            case DENY_UNSUPPORTED_DIALECT_FEATURE -> DecisionGuidance.retryable(
                "The query uses features unsupported by the selected dialect.",
                "use_supported_syntax",
                "Rewrite the query using syntax supported by the target dialect."
            );
            case DENY_PIPELINE_ERROR -> DecisionGuidance.terminal(
                "The middleware failed to process this query.",
                "report_pipeline_error"
            );
            default -> null;
        };
    }
}
