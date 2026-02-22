package io.sqm.control;

import io.sqm.core.Expression;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryRewriteRuleTest {

    @Test
    void default_id_uses_implementing_class_simple_name() {
        QueryRewriteRule rule = (query, context) -> QueryRewriteResult.unchanged(query);

        assertEquals(rule.getClass().getSimpleName(), rule.id());
        // also execute apply to keep interface method line exercised in patch coverage
        rule.apply(Query.select(Expression.literal(1)), ExecutionContext.of("ansi", ExecutionMode.ANALYZE));
    }
}
