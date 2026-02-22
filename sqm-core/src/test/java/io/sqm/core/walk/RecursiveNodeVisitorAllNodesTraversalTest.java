package io.sqm.core.walk;

import io.sqm.core.ColumnExpr;
import io.sqm.core.ComparisonOperator;
import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecursiveNodeVisitorAllNodesTraversalTest {

    @Test
    void selectQuery_covers_most_nodes() {
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

        var v = new RecordingVisitor();
        q.accept(v);

        var expected = Set.of(
            "SelectQuery",
            "ExprSelectItem", "StarSelectItem", "QualifiedStarSelectItem",
            "CaseExpr", "WhenThen",
            "ColumnExpr",
            "FunctionExpr", "PartitionBy", "Def", "Ref", "Preceding", "WindowDef", "Following", "CurrentRow", "UnboundedFollowing", "UnboundedPreceding",
            "LiteralExpr",
            "InPredicate", "AndPredicate", "OrPredicate", "AnyAllPredicate",
            "ComparisonPredicate", "IsNullPredicate", "LikePredicate",
            "GroupBy", "SimpleGroupItem",
            "OrderBy", "OrderItem",
            "LimitOffset",
            "OnJoin", "NaturalJoin", "CrossJoin", "UsingJoin",
            "Table"
        );

        assertTrue(v
                .seen()
                .containsAll(expected),
            () -> "Missing nodes: " + expected
                .stream()
                .filter(e -> !v
                    .seen()
                    .contains(e))
                .toList());
    }

    @Test
    void visitColumnExpr_shouldBeCalled_fromExpressionLeafNodes() {
        Query q =
            select(
                kase(when(col("u", "name").gt(10)).then(col("o", "name"))),
                col("o", "status"),
                func("count", arg(col("u", "id"))).as("cnt"),
                func("lower", arg(func("sub", arg(col("u", "desc"))))).as("lwr")
            )
                .from(tbl("orders").as("o"))
                .join(
                    inner(tbl("users").as("u")).on(col("u", "sid").eq(col("o", "user_id")))
                )
                .where(col("o", "state").in("A", "B"))
                .groupBy(group("u", "user_name"), group("o", "user_status"))
                .having(func("count", arg(col("u", "test"))).gt(10))
                .build();

        var expectedColumns = Set.of("u.name", "o.name", "o.status", "u.id", "u.desc", "u.sid", "o.user_id", "o.state", "u.user_name",
            "o.user_status", "u.test");

        var collector = new RecursiveNodeVisitor<Void>() {
            private final Set<String> columns = new LinkedHashSet<>();

            @Override
            protected Void defaultResult() {
                return null;
            }

            @Override
            public Void visitColumnExpr(ColumnExpr c) {
                columns.add(c.tableAlias() == null ? c.name() : c.tableAlias() + "." + c.name());
                return super.visitColumnExpr(c);
            }
        };

        q.accept(collector);

        assertEquals(expectedColumns, collector.columns);
    }
}
