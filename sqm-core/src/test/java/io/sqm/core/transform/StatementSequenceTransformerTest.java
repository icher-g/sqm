package io.sqm.core.transform;

import io.sqm.core.LiteralExpr;
import io.sqm.core.Node;
import io.sqm.core.StatementSequence;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static org.junit.jupiter.api.Assertions.*;

class StatementSequenceTransformerTest {

    @Test
    void unchangedStatementsPreserveSequenceIdentity() {
        var sequence = StatementSequence.of(select(lit(1)).build());

        var transformed = sequence.accept(new RecursiveNodeTransformer() {
        });

        assertSame(sequence, transformed);
    }

    @Test
    void changedStatementsProduceNewSequence() {
        var sequence = StatementSequence.of(select(lit(1)).build());

        var transformed = (StatementSequence) sequence.accept(new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit(2);
            }
        });

        assertNotSame(sequence, transformed);
        assertEquals(2, transformed.statements().getFirst().matchStatement()
            .query(q -> q.matchQuery()
                .select(s -> s.items().getFirst().matchSelectItem()
                    .expr(i -> i.expr().matchExpression()
                        .literal(LiteralExpr::value)
                        .orElse(null))
                    .orElse(null))
                .orElse(null))
            .orElse(null));
    }
}
