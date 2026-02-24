package io.sqm.control;

import io.sqm.control.rewrite.IdentifierNormalizationRewriteRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierNormalizationRewriteRuleTest {
    private static final ExecutionContext POSTGRES_ANALYZE = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

    @Test
    void rewrites_unquoted_identifiers() {
        var query = SqlQueryParser.standard().parse("select U.ID from Public.Users as U", POSTGRES_ANALYZE);

        var result = IdentifierNormalizationRewriteRule.of().apply(query, POSTGRES_ANALYZE);

        assertTrue(result.rewritten());
        assertEquals(ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION, result.primaryReasonCode());
        assertEquals("identifier-normalization", IdentifierNormalizationRewriteRule.of().id());
    }

    @Test
    void leaves_quoted_identifiers_unchanged() {
        var query = SqlQueryParser.standard().parse("select \"U\".\"ID\" from \"Public\".\"Users\" as \"U\"", POSTGRES_ANALYZE);

        var result = IdentifierNormalizationRewriteRule.of().apply(query, POSTGRES_ANALYZE);

        assertFalse(result.rewritten());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void validates_null_arguments() {
        var rule = IdentifierNormalizationRewriteRule.of();
        var query = SqlQueryParser.standard().parse("select 1", POSTGRES_ANALYZE);

        assertThrows(NullPointerException.class, () -> rule.apply(null, POSTGRES_ANALYZE));
        assertThrows(NullPointerException.class, () -> rule.apply(query, null));
    }
}
