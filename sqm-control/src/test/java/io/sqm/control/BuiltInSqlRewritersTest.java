package io.sqm.control;

import io.sqm.control.rewrite.BuiltInSqlRewriters;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInSqlRewritersTest {

    @Test
    void all_available_returns_noop_until_rules_are_wired() {
        var result = BuiltInSqlRewriters.allAvailable()
            .rewrite(io.sqm.core.Query.select(io.sqm.core.Expression.literal(1)),
                ExecutionContext.of("ansi", ExecutionMode.ANALYZE));

        assertFalse(result.rewritten());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void available_rules_is_currently_empty_and_immutable() {
        Set<BuiltInRewriteRule> available = BuiltInSqlRewriters.availableRules();

        assertTrue(available.isEmpty());
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () -> available.add(BuiltInRewriteRule.LIMIT_INJECTION));
    }

    @Test
    void selecting_unavailable_built_in_rule_fails_fast() {
        assertThrows(IllegalArgumentException.class,
            () -> BuiltInSqlRewriters.of(BuiltInRewriteRule.LIMIT_INJECTION));
        assertThrows(IllegalArgumentException.class,
            () -> BuiltInSqlRewriters.of(EnumSet.of(BuiltInRewriteRule.CANONICALIZATION)));
    }

    @Test
    void factory_validates_nulls_and_supports_empty_selection() {
        var query = io.sqm.core.Query.select(io.sqm.core.Expression.literal(1));
        var context = ExecutionContext.of("ansi", ExecutionMode.ANALYZE);

        assertThrows(NullPointerException.class, () -> BuiltInSqlRewriters.of((BuiltInRewriteRule[]) null));
        assertThrows(NullPointerException.class, () -> BuiltInSqlRewriters.of((Set<BuiltInRewriteRule>) null));

        var resultFromVarargs = BuiltInSqlRewriters.of().rewrite(query, context);
        var resultFromSet = BuiltInSqlRewriters.of(Set.of()).rewrite(query, context);
        assertFalse(resultFromVarargs.rewritten());
        assertFalse(resultFromSet.rewritten());
    }
}
