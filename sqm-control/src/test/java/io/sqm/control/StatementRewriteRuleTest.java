package io.sqm.control;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.pipeline.StatementRewriteResult;
import io.sqm.control.pipeline.StatementRewriteRule;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementRewriteRuleTest {

    @Test
    void default_id_uses_implementing_class_simple_name() {
        StatementRewriteRule rule = (statement, context) -> StatementRewriteResult.unchanged(statement);

        assertEquals(rule.getClass().getSimpleName(), rule.id());
        rule.apply(Query.select(Expression.literal(1)).build(), ExecutionContext.of("ansi", ExecutionMode.ANALYZE));
    }
}
