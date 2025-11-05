package io.sqm.core.walk;

import io.sqm.core.ColumnExpr;
import io.sqm.core.FunctionExpr;
import io.sqm.core.RowExpr;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts a depth-first (pre-order) traversal for a small Expression tree.
 * <p>
 * Tree shape (example):
 * RowExpr
 * ├─ FunctionExpr lower( ColumnArg( ColumnExpr u.id ) )
 * └─ ColumnExpr name
 * <p>
 * Expected visit order (one reasonable strategy):
 * RowExpr -> FunctionExpr -> FunctionExpr.Arg.Column -> ColumnExpr -> ColumnExpr
 *
 * <p>Note: Exact intermediate nodes visited depend on your implementation.
 * Adjust expectations accordingly.</p>
 */
public class TraversalOrderTest {

    @Test
    void visits_inDepthFirstOrder() {
        ColumnExpr colUid = ColumnExpr.of("u", "id");
        ColumnExpr colName = ColumnExpr.of(null, "name");

        FunctionExpr.Arg colArg = FunctionExpr.Arg.expr(colUid);
        FunctionExpr lower = FunctionExpr.of("lower", colArg);

        RowExpr row = RowExpr.of(List.of(lower, colName));

        TracingVisitor v = new TracingVisitor();
        row.accept(v);

        // Adjust the expected order if your visitor visits additional nodes (expr.g., args wrappers)
        List<String> expectedStart = List.of(
            "RowExpr",
            "FunctionExpr:lower",
            "ColumnExpr:u.id",
            "ColumnExpr:name"
        );

        // We only assert the prefix order as implementations may record extra nodes.
        assertTrue(v.order.size() >= expectedStart.size(), "Visitor should record at least the core nodes");

        for (int i = 0; i < expectedStart.size(); i++) {
            assertEquals(expectedStart.get(i), v.order.get(i), "Mismatch at index " + i + " in traversal order");
        }
    }

    static class TracingVisitor extends RecursiveNodeVisitor<Void> {
        final List<String> order = new ArrayList<>();

        @Override
        public Void visitRowExpr(RowExpr v) {
            order.add("RowExpr");
            return super.visitRowExpr(v); // must delegate to preserve recursion
        }

        @Override
        public Void visitFunctionExpr(FunctionExpr f) {
            order.add("FunctionExpr:" + f.name());
            return super.visitFunctionExpr(f);
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            order.add("ColumnExpr:" + (c.tableAlias() == null ? "" : c.tableAlias() + ".") + c.name());
            return super.visitColumnExpr(c);
        }
    }
}
