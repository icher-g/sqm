package io.sqm.transpile.builtin;

import io.sqm.core.Statement;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.dsl.Dsl;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class PostgresMergeUnsupportedRuleTest {

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), java.util.Optional.empty(), java.util.Optional.empty());
    }

    @Test
    void leavesBaselineStatementsUnchanged() {
        Statement statement = Dsl.select(Dsl.col("id")).from(Dsl.tbl("users")).build();

        var result = new PostgresMergeUnsupportedRule().apply(statement, context(SqlDialectId.POSTGRESQL, SqlDialectId.SQLSERVER));

        assertFalse(result.changed());
        assertSame(statement, result.statement());
        assertEquals("No PostgreSQL MERGE usage detected", result.description());
    }

    @Test
    void rejectsMergeForNonPostgresTargets() {
        Statement statement = Dsl.merge("users")
            .source(Dsl.tbl("src").as("s"))
            .on(Dsl.col("users", "id").eq(Dsl.col("s", "id")))
            .whenMatchedDoNothing()
            .whenNotMatchedBySourceDoNothing(Dsl.col("users", "active").eq(Dsl.lit(true)))
            .result(Dsl.col("users", "id").toSelectItem())
            .build();

        var result = new PostgresMergeUnsupportedRule().apply(statement, context(SqlDialectId.POSTGRESQL, SqlDialectId.SQLSERVER));

        assertEquals(io.sqm.transpile.RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertFalse(result.problems().isEmpty());
        assertEquals("UNSUPPORTED_POSTGRES_MERGE", result.problems().getFirst().code());
    }
}
