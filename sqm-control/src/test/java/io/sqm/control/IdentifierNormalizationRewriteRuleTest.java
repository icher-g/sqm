package io.sqm.control;

import io.sqm.control.audit.*;
import io.sqm.control.config.*;
import io.sqm.control.decision.*;
import io.sqm.control.execution.*;
import io.sqm.control.pipeline.*;
import io.sqm.control.rewrite.*;
import io.sqm.control.service.*;

import io.sqm.core.Query;
import io.sqm.control.rewrite.IdentifierNormalizationRewriteRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierNormalizationRewriteRuleTest {
    private static final ExecutionContext POSTGRES_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    private static Query parseQuery(String sql) {
        return (Query) SqlStatementParser.standard().parse(sql, POSTGRES_ANALYZE);
    }

    @Test
    void rewrites_unquoted_identifiers() {
        var query = parseQuery("select U.ID from Public.Users as U");

        var result = IdentifierNormalizationRewriteRule.of().apply(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION, result.primaryReasonCode());
        assertEquals("identifier-normalization", IdentifierNormalizationRewriteRule.of().id());
    }

    @Test
    void leaves_quoted_identifiers_unchanged() {
        var query = parseQuery("select \"U\".\"ID\" from \"Public\".\"Users\" as \"U\"");

        var result = IdentifierNormalizationRewriteRule.of().apply(query, POSTGRES_ANALYZE);

        assertFalse(result.rewritten());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void validates_null_arguments() {
        var rule = IdentifierNormalizationRewriteRule.of();
        var query = parseQuery("select 1");

        assertThrows(NullPointerException.class, () -> rule.apply(null, POSTGRES_ANALYZE));
        assertThrows(NullPointerException.class, () -> rule.apply(query, null));
    }
}


