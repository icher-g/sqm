package io.sqm.core.walk;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecursiveNodeVisitorEdgeNodesTraversalTest {

    @Test
    void edgeNodes_areVisited() {
        var sub = select(sel(func("max", arg(col("t", "v"))))).from(tbl("t"));

        var withBody = select(sel(col("c"))).from(tbl("cte_source"));
        var withQuery = with(cte("w", withBody))
            .select(select(sel(col("w", "c"))).from(tbl(select(sel(lit(1)))
                .from(tbl("dual"))).as("w")));

        var union =
            select(sel(col("a")))
                .from(tbl("A"))
                .where(col("a").between(1, 10))
                .unionAll(
                    select(sel(col("b")))
                        .from(tbl("B"))
                        .where(col("b").eq(select(sel(col("x"))).from(tbl("X")))))
                .orderBy(order(col("b")).asc())
                .limit(5L);

        var q = select(
            sel(func("coalesce", arg(col("m", "n")), arg(expr(sub)))),
            sel(row(col("r", "c1"), col("r", "c2"))))
            .from(tbl(rows(
                row(lit(1), lit("X")),
                row(lit(2), lit("Y")))
            ).as("vt", "id", "name"))
            .where(exists(select(sel(lit(1))).from(tbl("dual")))
                .and(not(col("m", "n").isNull()))
                .and(unary(col("r", "ok"))))
            .orderBy(order(col("m", "n")).asc());

        var v = new RecordingVisitor();
        withQuery.accept(v);
        union.accept(v);
        q.accept(v);

        var expected = Set.of(
            "WithQuery", "CteDef", "CompositeQuery",
            "QueryExpr", "RowExpr", "RowListExpr",
            "BetweenPredicate", "ExistsPredicate", "UnaryPredicate", "NotPredicate",
            "QueryTable", "ValuesTable"
        );

        assertTrue(
            v
                .seen()
                .containsAll(expected),
            () -> "Missing nodes: " + expected
                .stream()
                .filter(e -> !v
                    .seen()
                    .contains(e))
                .toList()
        );
    }
}
