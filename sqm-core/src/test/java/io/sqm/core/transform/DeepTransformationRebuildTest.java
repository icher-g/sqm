package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.FunctionExpr;
import io.sqm.core.Node;
import io.sqm.core.RowExpr;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Ensures that a deep change (rename inside a function argument) is applied and
 * that only the minimum necessary part of the tree is rebuilt.
 * <p>
 * Depending on your implementation, parent nodes may be rebuilt when any child changes.
 * This test asserts conservative expectations: children reflect the change,
 * siblings remain semantically equal, and object identity is preserved where no change occurred.
 */
public class DeepTransformationRebuildTest {

    @Test
    void deepChildChange_updatesOnlyNecessaryParts() {
        ColumnExpr colUid = ColumnExpr.of("u", "id");
        ColumnExpr colName = ColumnExpr.of(null, "name");

        // lower(u.id)
        FunctionExpr lower = FunctionExpr.of("lower", FunctionExpr.Arg.column(colUid));
        // Row: [ name, lower(u.id) ]
        RowExpr row = RowExpr.of(List.of(colName, lower));

        RenameInsideFunction t = new RenameInsideFunction();
        RowExpr out = (RowExpr) t.transform(row);

        // Left sibling should be preserved (identity) because it's unaffected
        assertSame(colName, out.items().get(0), "Unchanged left sibling should be the same instance");

        // Right child (function) should reflect the change in its arg
        FunctionExpr outFn = (FunctionExpr) out.items().get(1);
        assertEquals("lower", outFn.name());

        ColumnExpr outArgCol = ((FunctionExpr.Arg.Column) outFn.args().get(0)).ref();
        assertEquals("u", outArgCol.tableAlias());
        assertEquals("user_id", outArgCol.name(), "Deep-renamed column should be applied");
    }

    static class RenameInsideFunction extends RecursiveNodeTransformer {
        @Override
        public Node visitColumnExpr(ColumnExpr c) {
            if ("u".equals(c.tableAlias()) && "id".equals(c.name())) {
                return ColumnExpr.of("u", "user_id");
            }
            return c;
        }
    }
}
