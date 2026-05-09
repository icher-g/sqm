package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.currentValue;
import static io.sqm.dsl.Dsl.nextValue;
import static io.sqm.dsl.Dsl.qualify;
import static org.junit.jupiter.api.Assertions.*;

class SequenceValueExprTest {
    @Test
    void createsNextAndCurrentValueExpressions() {
        var next = nextValue("users_seq");
        var current = currentValue(qualify("app", "users_seq"));

        assertEquals(SequenceValueKind.NEXT_VALUE, next.kind());
        assertEquals(java.util.List.of("users_seq"), next.sequence().values());
        assertEquals(SequenceValueKind.CURRENT_VALUE, current.kind());
        assertEquals(java.util.List.of("app", "users_seq"), current.sequence().values());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> SequenceValueExpr.of(null, SequenceValueKind.NEXT_VALUE));
        assertThrows(NullPointerException.class, () -> SequenceValueExpr.of(QualifiedName.of("users_seq"), null));
    }

    @Test
    void dispatchesVisitorAndTransformerAsLeaf() {
        var expr = nextValue("users_seq");
        var visitor = new RecursiveNodeVisitor<String>() {
            @Override
            protected String defaultResult() {
                return "default";
            }

            @Override
            public String visitSequenceValueExpr(SequenceValueExpr expr) {
                return expr.kind().name();
            }
        };
        var transformer = new RecursiveNodeTransformer() {
        };

        assertEquals("NEXT_VALUE", expr.accept(visitor));
        assertSame(expr, transformer.transform(expr));
    }

    @Test
    void expressionMatcherMatchesSequenceValue() {
        var out = nextValue("users_seq").matchExpression()
            .column(column -> "column")
            .sequenceValue(sequence -> sequence.kind().name())
            .orElse("other");

        assertEquals("NEXT_VALUE", out);
    }
}
