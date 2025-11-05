package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.FunctionExpr;
import io.sqm.core.RowExpr;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that a no-op transformer preserves object identity for the whole subtree.
 * (I.expr., if nothing changes, the same instances are returned.)
 */
public class NoopPreservesIdentityTest {

    @Test
    void unchangedTree_returnsSameInstances() {
        ColumnExpr colUid = ColumnExpr.of("u", "id");
        ColumnExpr colName = ColumnExpr.of(null, "name");
        FunctionExpr lower = FunctionExpr.of("lower", FunctionExpr.Arg.expr(colName));
        RowExpr row = RowExpr.of(List.of(colUid, lower));

        NoopTransformer t = new NoopTransformer();
        RowExpr out = (RowExpr) t.transform(row);

        assertSame(row, out, "RowExpr instance should be preserved when unchanged");
        assertSame(colUid, out.items().get(0), "Left child should be preserved");
        FunctionExpr outFn = (FunctionExpr) out.items().get(1);
        assertSame(lower, outFn, "FunctionExpr instance should be preserved");
        // Assuming args() returns an immutable list with same Arg instances
        assertSame(lower.args().get(0), outFn.args().get(0), "Function arg should be preserved");
    }

    static class NoopTransformer extends RecursiveNodeTransformer {
        // No overrides — rely entirely on the superclass traversal.
    }
}
