package io.sqm.core.transform;

import io.sqm.core.*;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

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
        FunctionExpr lower = FunctionExpr.of("lower", FunctionExpr.Arg.expr(colUid));
        // Row: [ name, lower(u.id) ]
        RowExpr row = RowExpr.of(List.of(colName, lower));

        RenameInsideFunction t = new RenameInsideFunction();
        RowExpr out = (RowExpr) t.transform(row);

        // Left sibling should be preserved (identity) because it's unaffected
        assertSame(colName, out.items().get(0), "Unchanged left sibling should be the same instance");

        // Right child (function) should reflect the change in its arg
        FunctionExpr outFn = (FunctionExpr) out.items().get(1);
        assertEquals("lower", outFn.name());

        ColumnExpr outArgCol = ((FunctionExpr.Arg.ExprArg) outFn.args().getFirst()).expr().<ColumnExpr>matchExpression().column(c -> c).orElse(null);
        assertEquals("u", outArgCol.tableAlias());
        assertEquals("user_id", outArgCol.name(), "Deep-renamed column should be applied");
    }

    @Test
    void changeAllColumnExpressions_InQuery() {
        var q =
            select(
                kase(when(col("u", "name").gt(10)).then(col("o", "name"))),
                col("o", "status"),
                func("count", arg(col("u", "id")))
                    .over(
                        partition(col("acct_id")),
                        orderBy(order(col("ts")).asc()),
                        rows(preceding(5))
                    ).as("cnt"),
                func("lower", arg(func("sub", arg(col("u", "desc")))))
                    .over("w").as("lwr"),
                star(),
                star("o")
            )
                .from(tbl("orders").as("o"))
                .join(inner(tbl("users").as("u")).on(col("u", "sid").eq(col("o", "user_id"))))
                .join(natural(tbl("regions").as("r")))
                .join(cross(tbl("x").as("x1")))
                .join(inner(tbl("y").as("y1")).using("k1", "k2"))
                .where(
                    col("o", "state")
                        .in("A", "B")
                        .and(
                            func("count", arg(col("u", "id"))).gt(10)
                        )
                        .and(
                            col("o", "flag").isNull()
                                .or(col("o", "code").like("%ZZ%"))
                                .or(col("o", "user").all(ComparisonOperator.EQ, select(lit(1)).build()))
                        )
                )
                .groupBy(group("u", "user_name"), group("o", "user_status"))
                .having(func("count", arg(col("u", "test"))).gt(10))
                .window(
                    window("w", over(partition(col("acct_id")), rows(preceding(1), following(1)))),
                    window("w", over(partition(col("acct_id")), rows(currentRow()))),
                    window("w", over(partition(col("acct_id")), rows(unboundedFollowing()))),
                    window("w", over(partition(col("acct_id")), rows(unboundedPreceding())))
                )
                .orderBy(order(col("o", "status")).desc())
                .limit(100)
                .offset(10)
                .build();

        var transformer = new RenameAnyColumn();
        var newQuery = q.accept(transformer);
        var collector = new ColumnCollector();
        newQuery.accept(collector);
        assertTrue(collector.getColumns().stream().allMatch(c -> c.startsWith("dbo.")));
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

    static class RenameAnyColumn extends RecursiveNodeTransformer {
        @Override
        public Node visitColumnExpr(ColumnExpr c) {
            return ColumnExpr.of("dbo." + c.tableAlias(), c.name());
        }
    }

    static class ColumnCollector extends RecursiveNodeVisitor<Void> {
        private final Set<String> cols = new LinkedHashSet<>();

        public Set<String> getColumns() {
            return cols;
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            cols.add(c.tableAlias() == null ? c.name() : c.tableAlias() + "." + c.name());
            return super.visitColumnExpr(c); // continue recursion if there are nested nodes (usually none for columns)
        }
    }
}
