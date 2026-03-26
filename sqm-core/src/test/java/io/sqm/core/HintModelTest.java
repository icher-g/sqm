package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class HintModelTest {

    @Test
    void genericStatementHintPreservesTypedArguments() {
        StatementHint hint = StatementHint.of(
            "MAX_EXECUTION_TIME",
            HintArg.expression(Expression.literal(1000)),
            HintArg.identifier("FAST")
        );

        assertEquals(Identifier.of("MAX_EXECUTION_TIME"), hint.name());
        assertEquals(2, hint.args().size());
        assertInstanceOf(ExpressionHintArg.class, hint.args().get(0));
        assertInstanceOf(IdentifierHintArg.class, hint.args().get(1));
    }

    @Test
    void tableHelperCreatesGenericTypedHint() {
        var hint = TableHint.of("FORCE_INDEX", HintArg.identifier("idx_users_name"));

        assertEquals(Identifier.of("FORCE_INDEX"), hint.name());
        assertEquals(1, hint.args().size());
        assertEquals(Identifier.of("idx_users_name"), ((IdentifierHintArg) hint.args().getFirst()).value());
    }

    @Test
    void recursiveVisitorTraversesTableHintsAndHintArgs() {
        var table = TableRef.table(Identifier.of("users"))
            .withNoLock()
            .useIndex("idx_users_name");
        var visited = new ArrayList<String>();

        new RecursiveNodeVisitor<Void>() {
            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitTableHint(TableHint hint) {
                visited.add(hint.name().value());
                return super.visitTableHint(hint);
            }

            @Override
            public Void visitIdentifierHintArg(IdentifierHintArg arg) {
                visited.add(arg.value().value());
                return super.visitIdentifierHintArg(arg);
            }
        }.visitTable(table);

        assertEquals(List.of("NOLOCK", "USE_INDEX", "idx_users_name"), visited);
    }

    @Test
    void recursiveTransformerRebuildsGenericHintWhenExpressionArgumentChanges() {
        var original = StatementHint.of("MAX_EXECUTION_TIME", HintArg.expression(Expression.literal(1000)));

        var transformed = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if (Integer.valueOf(1000).equals(l.value())) {
                    return Expression.literal(2000);
                }
                return l;
            }
        }.transform(original);

        var rewritten = assertInstanceOf(StatementHint.class, transformed);
        var arg = assertInstanceOf(ExpressionHintArg.class, rewritten.args().getFirst());
        assertEquals(2000, assertInstanceOf(LiteralExpr.class, arg.value()).value());
    }

    @Test
    void recursiveTransformerPreservesIdentityWhenHintIsUnchanged() {
        var original = StatementHint.of("MAX_EXECUTION_TIME", HintArg.expression(Expression.literal(1000)));

        var transformed = new RecursiveNodeTransformer() {
        }.transform(original);

        assertSame(original, transformed);
    }
}
