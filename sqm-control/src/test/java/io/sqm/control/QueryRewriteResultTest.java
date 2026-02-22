package io.sqm.control;

import io.sqm.core.Expression;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryRewriteResultTest {

    @Test
    void unchanged_factory_creates_valid_result() {
        Query query = Query.select(Expression.literal(1));

        QueryRewriteResult result = QueryRewriteResult.unchanged(query);

        assertEquals(query, result.query());
        assertFalse(result.rewritten());
        assertEquals(List.of(), result.appliedRuleIds());
        assertEquals(ReasonCode.NONE, result.primaryReasonCode());
    }

    @Test
    void rewritten_factory_creates_valid_result() {
        Query query = Query.select(Expression.literal(1));

        QueryRewriteResult result = QueryRewriteResult.rewritten(query, "limit-injection", ReasonCode.REWRITE_LIMIT);

        assertEquals(query, result.query());
        assertTrue(result.rewritten());
        assertEquals(List.of("limit-injection"), result.appliedRuleIds());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.primaryReasonCode());
    }

    @Test
    void copies_applied_rule_ids_defensively() {
        Query query = Query.select(Expression.literal(1));
        List<String> ids = new ArrayList<>();
        ids.add("r1");

        QueryRewriteResult result = new QueryRewriteResult(query, true, ids, ReasonCode.REWRITE_LIMIT);
        ids.add("r2");

        assertEquals(List.of("r1"), result.appliedRuleIds());
        assertThrows(UnsupportedOperationException.class, () -> result.appliedRuleIds().add("r3"));
    }

    @Test
    void validates_constructor_arguments() {
        Query query = Query.select(Expression.literal(1));

        assertThrows(NullPointerException.class, () -> new QueryRewriteResult(null, false, List.of(), ReasonCode.NONE));
        assertThrows(NullPointerException.class, () -> new QueryRewriteResult(query, false, null, ReasonCode.NONE));
        assertThrows(NullPointerException.class, () -> new QueryRewriteResult(query, false, List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new QueryRewriteResult(query, false, List.of("r1"), ReasonCode.NONE));
        assertThrows(IllegalArgumentException.class, () -> new QueryRewriteResult(query, false, List.of(), ReasonCode.REWRITE_LIMIT));
        assertThrows(IllegalArgumentException.class, () -> new QueryRewriteResult(query, true, List.of(), ReasonCode.REWRITE_LIMIT));
        assertThrows(IllegalArgumentException.class, () -> new QueryRewriteResult(query, true, List.of(" "), ReasonCode.REWRITE_LIMIT));
        assertThrows(IllegalArgumentException.class, () -> new QueryRewriteResult(query, true, List.of("r1"), ReasonCode.NONE));
    }
}
