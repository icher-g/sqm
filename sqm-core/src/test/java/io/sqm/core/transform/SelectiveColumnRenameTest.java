package io.sqm.core.transform;

import io.sqm.core.ColumnExpr;
import io.sqm.core.FunctionExpr;
import io.sqm.core.Identifier;
import io.sqm.core.QualifiedName;
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
        ColumnExpr colUid = ColumnExpr.of(Identifier.of("u"), Identifier.of("id"));
        ColumnExpr colName = ColumnExpr.of(null, Identifier.of("name"));
        FunctionExpr lower = FunctionExpr.of(QualifiedName.of("lower"), List.of(FunctionExpr.Arg.expr(colName)), null, null, null, null, null);
        RowExpr row = RowExpr.of(List.of(colUid, lower));

        RowExpr out = IdentifierTransforms.renameColumn(row, "u", "id", "user_id");

        // Row is rebuilt if any child changed; check structural expectations
        assertEquals(2, out.items().size(), "Row must still have two items");

        ColumnExpr outCol0 = (ColumnExpr) out.items().getFirst();
        assertEquals("u", outCol0.tableAlias().value());
        assertEquals("user_id", outCol0.name().value(), "First item should be renamed");

        FunctionExpr outFn = (FunctionExpr) out.items().get(1);
        assertEquals("lower", outFn.name().values().getLast(), "Function name should remain the same");
        // The arg is a column(name), which should remain unchanged
        ColumnExpr argCol = ((FunctionExpr.Arg.ExprArg) outFn.args().getFirst()).expr().<ColumnExpr>matchExpression().column(c -> c).orElse(null);
        assertNull(argCol.tableAlias());
        assertEquals("name", argCol.name().value(), "Non-target column should remain unchanged");

        // Original colName instance may or may not be preserved depending on rebuilding strategy.
        // It's safe to check semantic equality only:
        assertEquals("name", argCol.name().value());
    }
}
