package io.sqm.control;

import io.sqm.control.decision.ReasonCode;
import io.sqm.control.pipeline.StatementRewriteResult;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementRewriteResultTest {

    @Test
    void unchanged_factory_creates_valid_result() {
        Statement statement = Query.select(Expression.literal(1)).build();

        StatementRewriteResult result = StatementRewriteResult.unchanged(statement);

        assertEquals(statement, result.statement());
        assertFalse(result.rewritten());
        assertEquals(List.of(), result.appliedRuleIds());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void rewritten_factory_creates_valid_result() {
        Statement statement = Query.select(Expression.literal(1)).build();

        StatementRewriteResult result = StatementRewriteResult.rewritten(statement, "limit-injection", ReasonCode.REWRITE_LIMIT);

        assertEquals(statement, result.statement());
        assertTrue(result.rewritten());
        assertEquals(List.of("limit-injection"), result.appliedRuleIds());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
    }

    @Test
    void copies_applied_rule_ids_defensively() {
        Statement statement = Query.select(Expression.literal(1)).build();
        List<String> ids = new ArrayList<>();
        ids.add("r1");

        StatementRewriteResult result = new StatementRewriteResult(statement, true, ids, ReasonCode.REWRITE_LIMIT);
        ids.add("r2");

        assertEquals(List.of("r1"), result.appliedRuleIds());
        assertThrows(UnsupportedOperationException.class, () -> result.appliedRuleIds().add("r3"));
    }

    @Test
    void validates_constructor_arguments() {
        Statement statement = Query.select(Expression.literal(1)).build();

        assertThrows(NullPointerException.class, () -> new StatementRewriteResult(null, false, List.of(), ReasonCode.NONE));
        assertThrows(NullPointerException.class, () -> new StatementRewriteResult(statement, false, null, ReasonCode.NONE));
        assertThrows(NullPointerException.class, () -> new StatementRewriteResult(statement, false, List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new StatementRewriteResult(statement, false, List.of("r1"), ReasonCode.NONE));
        assertThrows(IllegalArgumentException.class, () -> new StatementRewriteResult(statement, false, List.of(), ReasonCode.REWRITE_LIMIT));
        assertThrows(IllegalArgumentException.class, () -> new StatementRewriteResult(statement, true, List.of(), ReasonCode.REWRITE_LIMIT));
        assertThrows(IllegalArgumentException.class, () -> new StatementRewriteResult(statement, true, List.of(" "), ReasonCode.REWRITE_LIMIT));
        assertThrows(IllegalArgumentException.class, () -> new StatementRewriteResult(statement, true, List.of("r1"), ReasonCode.NONE));
    }
}
