package io.sqm.core;

import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static org.junit.jupiter.api.Assertions.*;

class StatementSequenceTest {

    @Test
    void copiesStatementsIntoImmutableList() {
        var query = select(lit(1)).build();
        var statements = new ArrayList<Statement>();
        statements.add(query);

        var sequence = StatementSequence.of(statements);
        statements.clear();

        assertEquals(1, sequence.statements().size());
        assertThrows(UnsupportedOperationException.class, () -> sequence.statements().add(query));
    }

    @Test
    void recursiveVisitorTraversesStatements() {
        var sequence = StatementSequence.of(
            select(lit(1)).build(),
            select(lit(2)).build()
        );
        var visitor = new CountingVisitor();

        sequence.accept(visitor);

        assertEquals(2, visitor.selectCount);
    }

    private static final class CountingVisitor extends RecursiveNodeVisitor<Void> {
        private int selectCount;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitSelectQuery(SelectQuery q) {
            selectCount++;
            return super.visitSelectQuery(q);
        }
    }
}
