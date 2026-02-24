package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.FunctionExpr;
import io.sqm.core.Identifier;
import io.sqm.core.QualifiedName;
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
        ColumnExpr colUid = ColumnExpr.of(Identifier.of("u"), Identifier.of("id"));
        ColumnExpr colName = ColumnExpr.of(null, Identifier.of("name"));
        FunctionExpr lower = FunctionExpr.of(QualifiedName.of("lower"), List.of(FunctionExpr.Arg.expr(colName)), null, null, null, null);
        RowExpr row = RowExpr.of(List.of(colUid, lower));

        NoopTransformer t = new NoopTransformer();
        RowExpr out = (RowExpr) t.transform(row);

        assertSame(row, out, "RowExpr instance should be preserved when unchanged");
        assertSame(colUid, out.items().get(0), "Left child should be preserved");
        FunctionExpr outFn = (FunctionExpr) out.items().get(1);
        assertSame(lower, outFn, "FunctionExpr instance should be preserved");
        // Assuming args() returns an immutable list with same Arg instances
        assertSame(lower.args().getFirst(), outFn.args().getFirst(), "Function arg should be preserved");
    }

    static class NoopTransformer extends RecursiveNodeTransformer {
        // No overrides — rely entirely on the superclass traversal.
    }
}

