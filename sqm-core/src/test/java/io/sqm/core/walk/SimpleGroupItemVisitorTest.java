package io.sqm.core.walk;

import io.sqm.core.ColumnExpr;
import io.sqm.core.GroupItem;
import io.sqm.core.Node;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleGroupItemVisitorTest {

    @Test
    void visitsGroupItemExpression() {
        var item = GroupItem.of(col("a"));
        var visitor = new ColumnFlagVisitor();

        item.accept(visitor);

        assertTrue(visitor.seenColumn);
    }

    @Test
    void acceptNullReturnsNull() {
        var visitor = new ColumnFlagVisitor();
        assertNull(visitor.acceptNode(null));
    }

    private static final class ColumnFlagVisitor extends RecursiveNodeVisitor<Void> {
        private boolean seenColumn;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            seenColumn = true;
            return null;
        }

        public Void acceptNode(Node n) {
            return accept(n);
        }
    }
}
