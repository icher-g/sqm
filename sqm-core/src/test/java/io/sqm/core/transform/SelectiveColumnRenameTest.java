package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.FunctionExpr;
import io.sqm.core.Node;
import io.sqm.core.RowExpr;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Demonstrates a targeted leaf change (rename of column u.id -> u.user_id)
 * and asserts that only the intended nodes are replaced.
 */
public class SelectiveColumnRenameTest {

    @Test
    void renamesOnlyTargetColumn_andKeepsOthers() {
        ColumnExpr colUid = ColumnExpr.of("u", "id");
        ColumnExpr colName = ColumnExpr.of(null, "name");
        FunctionExpr lower = FunctionExpr.of("lower", FunctionExpr.Arg.expr(colName));
        RowExpr row = RowExpr.of(List.of(colUid, lower));

        RenameColumnTransformer t = new RenameColumnTransformer();
        RowExpr out = (RowExpr) t.transform(row);

        // Row is rebuilt if any child changed; check structural expectations
        assertEquals(2, out.items().size(), "Row must still have two items");

        ColumnExpr outCol0 = (ColumnExpr) out.items().get(0);
        assertEquals("u", outCol0.tableAlias());
        assertEquals("user_id", outCol0.name(), "First item should be renamed");

        FunctionExpr outFn = (FunctionExpr) out.items().get(1);
        assertEquals("lower", outFn.name(), "Function name should remain the same");
        // The arg is a column(name), which should remain unchanged
        ColumnExpr argCol = ((FunctionExpr.Arg.ExprArg) outFn.args().get(0)).expr().asColumn().orElseThrow();
        assertNull(argCol.tableAlias());
        assertEquals("name", argCol.name(), "Non-target column should remain unchanged");

        // Original colName instance may or may not be preserved depending on rebuilding strategy.
        // It's safe to check semantic equality only:
        assertEquals("name", argCol.name());
    }

    static class RenameColumnTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitColumnExpr(ColumnExpr c) {
            if ("u".equals(c.tableAlias()) && "id".equals(c.name())) {
                return ColumnExpr.of("u", "user_id");
            }
            return c;
        }
    }
}
