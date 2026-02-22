package io.sqm.core.transform;

import io.sqm.core.*;
import io.sqm.core.utils.Literals;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class RecursiveNodeTransformerTest {

    @Test
    void visitCaseExpr() {
        var kase = kase(when(col("c").eq(1)).then(5)).elseValue(2);
        var thenTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitWhenThen(WhenThen w) {
                return when(w.when()).then(10);
            }
        };
        var case1 = (CaseExpr) kase.accept(thenTransformer);
        assertEquals(10, case1.whens().getFirst().then().matchExpression().literal(l -> l.value()).orElse(false));
        var elseTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr e) {
                if (e.value().equals(2)) {
                    return lit(100);
                }
                return e;
            }
        };
        var case2 = (CaseExpr) kase.accept(elseTransformer);
        assertEquals(100, case2.elseExpr().matchExpression().literal(l -> l.value()).orElse(false));
        var case3 = (CaseExpr) kase.accept(new NothingTransformer());
        assertEquals(5, case3.whens().getFirst().then().matchExpression().literal(l -> l.value()).orElse(false));
        assertEquals(2, case3.elseExpr().matchExpression().literal(l -> l.value()).orElse(false));
    }

    @Test
    void visitColumnExpr() {
        var column = col("c");
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col("t", c.name());
            }
        };
        var c = (ColumnExpr) column.accept(columnTransformer);
        assertEquals("t", c.tableAlias());
        assertEquals("c", c.name());
    }

    @Test
    void visitFunctionExpr() {
        var func = func("count", starArg()).withinGroup(OrderItem.of(1));
        var groupTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitOrderItem(OrderItem i) {
                return OrderItem.of(2);
            }
        };
        var f1 = (FunctionExpr) func.accept(groupTransformer);
        assertEquals(2, f1.withinGroup().items().getFirst().ordinal());
        func = func.filter(col("c").in(1, 2, 3));
        var filterTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitRowExpr(RowExpr v) {
                return row(4, 5, 6);
            }
        };
        var f2 = (FunctionExpr) func.accept(filterTransformer);

        var values = f2.filter().matchPredicate()
            .in(p -> p.rhs().<List<Integer>>matchValueSet()
                .row(r -> r.items().stream().map(i -> i.<Integer>matchExpression()
                        .literal(l -> (Integer) l.value())
                        .orElse(null)
                    ).toList()
                )
                .orElse(null)
            )
            .orElse(null);

        assertEquals(List.of(4, 5, 6), values);
        var f3 = (FunctionExpr) func.accept(new NothingTransformer());
        assertEquals(1, f3.withinGroup().items().getFirst().ordinal());
        assertEquals(List.of(1, 2, 3), f3.filter().matchPredicate()
            .in(p -> p.rhs().matchValueSet()
                .row(r -> r.items().stream().map(i -> i.matchExpression()
                        .literal(l -> l.value())
                        .orElse(null)
                    ).toList()
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void visitFunctionArgExpr() {
    }

    @Test
    void visitLiteralExpr() {
    }

    @Test
    void visitRowExpr() {
    }

    @Test
    void visitQueryExpr() {
        var queryExpr = expr(select(lit(1)).build());
        var queryTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitExprSelectItem(ExprSelectItem i) {
                return lit(2).toSelectItem();
            }
        };
        var q1 = (QueryExpr) queryExpr.accept(queryTransformer);
        assertEquals(2, q1.subquery().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var q2 = (QueryExpr) queryExpr.accept(new NothingTransformer());
        assertEquals(1, q2.subquery().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void visitRowListExpr() {
        var rows = rows(row(1, 2, 3));
        var rowTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitRowExpr(RowExpr v) {
                return row(4, 5, 6);
            }
        };
        var rows2 = (RowListExpr) rows.accept(rowTransformer);
        assertNotEquals(rows.rows(), rows2.rows());
        var rows3 = (RowListExpr) rows.accept(new NothingTransformer());
        assertEquals(rows.rows(), rows3.rows());
    }

    @Test
    void visitExprSelectItem() {
    }

    @Test
    void visitStarSelectItem() {
    }

    @Test
    void visitQualifiedStarSelectItem() {
    }

    @Test
    void visitTable() {
    }

    @Test
    void visitQueryTable() {
        var qt = tbl(select(lit(1)).build());
        var transformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit(2);
            }
        };
        var qt2 = (QueryTable) qt.accept(transformer);
        assertEquals(2, qt2.query().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var qt3 = (QueryTable) qt.accept(new NothingTransformer());
        assertEquals(1, qt3.query().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void visitValuesTable() {
        var vt = tbl(rows(row(1, 2, 3)));
        var rowTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitRowExpr(RowExpr v) {
                return row(4, 5, 6);
            }
        };
        var vt2 = (ValuesTable) vt.accept(rowTransformer);
        assertNotEquals(vt.values(), vt2.values());
        var vt3 = (ValuesTable) vt.accept(new NothingTransformer());
        assertEquals(vt.values(), vt3.values());
    }

    @Test
    void visitOnJoin() {
        var join = inner(tbl("t1")).on(col("c1").eq(10));
        var tableTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table t) {
                return tbl("t2");
            }
        };
        var join2 = (OnJoin) join.accept(tableTransformer);
        assertEquals("t2", join2.right().matchTableRef().table(t -> t.name()).orElse(null));
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col("c2");
            }
        };
        var join3 = (OnJoin) join.accept(columnTransformer);
        assertEquals("c2", join3.on().matchPredicate()
            .comparison(cmp -> cmp.lhs().matchExpression()
                .column(c -> c.name())
                .orElse(null)
            )
            .orElse(null)
        );
        var join4 = (OnJoin) join.accept(new NothingTransformer());
        assertEquals(join, join4);
    }

    @Test
    void visitCrossJoin() {
        var join = cross(tbl("t1"));
        var tableTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table t) {
                return tbl("t2");
            }
        };
        var join2 = (CrossJoin) join.accept(tableTransformer);
        assertEquals("t2", join2.right().matchTableRef().table(t -> t.name()).orElse(null));
        var join3 = (CrossJoin) join.accept(new NothingTransformer());
        assertEquals(join, join3);
    }

    @Test
    void visitNaturalJoin() {
        var join = natural(tbl("t1"));
        var tableTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table t) {
                return tbl("t2");
            }
        };
        var join2 = (NaturalJoin) join.accept(tableTransformer);
        assertEquals("t2", join2.right().matchTableRef().table(t -> t.name()).orElse(null));
        var join3 = (NaturalJoin) join.accept(new NothingTransformer());
        assertEquals(join, join3);
    }

    @Test
    void visitUsingJoin() {
        var join = inner(tbl("t1")).using("a");
        var tableTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table t) {
                return tbl("t2");
            }
        };
        var join2 = (UsingJoin) join.accept(tableTransformer);
        assertEquals("t2", join2.right().matchTableRef().table(t -> t.name()).orElse(null));
        var join3 = (UsingJoin) join.accept(new NothingTransformer());
        assertEquals(join, join3);
    }

    @Test
    void visitGroupBy() {
        var gb = GroupBy.of(List.of(group("c1"), group("c2")));
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var gb2 = (GroupBy) gb.accept(columnTransformer);
        assertEquals(List.of("c11", "c21"), gb2.items().stream()
            .map(i -> ((GroupItem.SimpleGroupItem) i).expr().matchExpression()
                .column(c -> c.name())
                .orElse(null)
            )
            .toList()
        );
        var gb3 = (GroupBy) gb.accept(new NothingTransformer());
        assertEquals(gb, gb3);
    }

    @Test
    void visitGroupItem() {
    }

    @Test
    void visitSimpleGroupItem() {
        var item = (GroupItem.SimpleGroupItem) group("c1");
        var transformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + "x");
            }
        };

        var transformed = (GroupItem.SimpleGroupItem) item.accept(transformer);
        assertNotSame(item, transformed);
        assertEquals("c1x", transformed.expr().matchExpression()
            .column(c -> c.name())
            .orElse(null));

        var unchanged = (GroupItem.SimpleGroupItem) item.accept(new NothingTransformer());
        assertSame(item, unchanged);
    }

    @Test
    void visitGroupingSets() {
        var item = groupingSets(
            groupingSet(group("c1")),
            groupingSet(group("c2")),
            groupingSet()
        );
        var transformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + "x");
            }
        };

        var transformed = (GroupItem) item.accept(transformer);
        assertInstanceOf(GroupItem.GroupingSets.class, transformed);
        var sets = (GroupItem.GroupingSets) transformed;
        var first = (GroupItem.GroupingSet) sets.sets().getFirst();
        assertEquals("c1x", ((GroupItem.SimpleGroupItem) first.items().getFirst()).expr().matchExpression()
            .column(c -> c.name())
            .orElse(null));
    }

    @Test
    void visitRollup() {
        var rollup = rollup(group("c1"), group("c2"));
        var transformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + "x");
            }
        };

        var transformed = (GroupItem.Rollup) rollup.accept(transformer);
        assertEquals("c1x", ((GroupItem.SimpleGroupItem) transformed.items().getFirst()).expr().matchExpression()
            .column(c -> c.name())
            .orElse(null));
    }

    @Test
    void visitCube() {
        var cube = cube(group("c1"));
        var transformed = (GroupItem.Cube) cube.accept(new NothingTransformer());
        assertSame(cube, transformed);
    }

    @Test
    void visitOrderBy() {
    }

    @Test
    void visitOrderItem() {
    }

    @Test
    void visitLimitOffset() {
    }

    @Test
    void visitAnyAllPredicate() {
        var any = col("c1").any(ComparisonOperator.EQ, select(lit(1)).build());
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var any2 = (AnyAllPredicate) any.accept(columnTransformer);
        assertEquals("c11", any2.lhs().matchExpression().column(c -> c.name()).orElse(null));
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit((Integer) l.value() + 1);
            }
        };
        var any3 = (AnyAllPredicate) any.accept(literalTransformer);
        assertEquals(2, any3.subquery().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var any4 = (AnyAllPredicate) any.accept(new NothingTransformer());
        assertEquals(any, any4);
    }

    @Test
    void visitBetweenPredicate() {
        var p = col("c1").between(1, 5);
        // change value
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var p1 = (BetweenPredicate) p.accept(columnTransformer);
        assertEquals("c11", p1.value().matchExpression().column(c -> c.name()).orElse(null));
        // change lower value
        var literalTransformer1 = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                var num = (Integer) l.value();
                if (num == 1) {
                    return lit(10 + num);
                }
                return l;
            }
        };
        var p2 = (BetweenPredicate) p.accept(literalTransformer1);
        assertEquals(11, p2.lower().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(5, p2.upper().matchExpression().literal(l -> l.value()).orElse(null));
        // change upper value
        var literalTransformer2 = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                var num = (Integer) l.value();
                if (num == 5) {
                    return lit(10 + num);
                }
                return l;
            }
        };
        assertEquals("c11", p1.value().matchExpression().column(c -> c.name()).orElse(null));
        var p3 = (BetweenPredicate) p.accept(literalTransformer2);
        assertEquals(1, p3.lower().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(15, p3.upper().matchExpression().literal(l -> l.value()).orElse(null));
        // change nothing
        var p4 = (BetweenPredicate) p.accept(new NothingTransformer());
        assertEquals(p, p4);
    }

    @Test
    void visitComparisonPredicate() {
        var p = col("c1").eq(10);
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var p1 = (ComparisonPredicate) p.accept(columnTransformer);
        assertEquals("c11", p1.lhs().matchExpression().column(c -> c.name()).orElse(null));
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit((Integer) l.value() + 1);
            }
        };
        var p2 = (ComparisonPredicate) p.accept(literalTransformer);
        assertEquals(11, p2.rhs().matchExpression().literal(l -> l.value()).orElse(null));
        var p3 = (ComparisonPredicate) p.accept(new NothingTransformer());
        assertEquals(p, p3);
    }

    @Test
    void visitExistsPredicate() {
        var p = exists(select(lit(1)).build());
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit((Integer) l.value() + 1);
            }
        };
        var p1 = (ExistsPredicate) p.accept(literalTransformer);
        assertEquals(2, p1.subquery().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var p2 = (ExistsPredicate) p.accept(new NothingTransformer());
        assertEquals(p, p2);
    }

    @Test
    void visitInPredicate() {
    }

    @Test
    void visitIsNullPredicate() {
        var p = col("c1").isNull();
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var p1 = (IsNullPredicate) p.accept(columnTransformer);
        assertEquals("c11", p1.expr().matchExpression().column(c -> c.name()).orElse(null));
        var p2 = (IsNullPredicate) p.accept(new NothingTransformer());
        assertEquals(p, p2);
    }

    @Test
    void visitLikePredicate() {
        var p = col("c1").like("%c%").escape("\\");
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var p1 = (LikePredicate) p.accept(columnTransformer);
        assertEquals("c11", p1.value().matchExpression().column(c -> c.name()).orElse(null));
        var patternTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if ("%c%".equals(l.value())) {
                    return lit("%d%");
                }
                return super.visitLiteralExpr(l);
            }
        };
        var p2 = (LikePredicate) p.accept(patternTransformer);
        assertEquals("%d%", p2.pattern().matchExpression().literal(l -> l.value()).orElse(null));
        var escapeTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if ("\\".equals(l.value())) {
                    return lit("//");
                }
                return super.visitLiteralExpr(l);
            }
        };
        var p3 = (LikePredicate) p.accept(escapeTransformer);
        assertEquals("//", p3.escape().matchExpression().literal(l -> l.value()).orElse(null));
        var p4 = (LikePredicate) p.accept(new NothingTransformer());
        assertEquals(p, p4);
    }

    @Test
    void visitNotPredicate() {
        var p = not(col("c1").eq(1));
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var p1 = (NotPredicate) p.accept(columnTransformer);
        assertEquals("c11", p1.inner().matchPredicate()
            .comparison(cmp -> cmp.lhs().matchExpression()
                .column(c -> c.name())
                .orElse(null)
            )
            .orElse(null)
        );
        var p2 = (NotPredicate) p.accept(new NothingTransformer());
        assertEquals(p, p2);
    }

    @Test
    void visitUnaryPredicate() {
        var p = unary(lit(true));
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit(false);
            }
        };
        var p1 = (UnaryPredicate) p.accept(literalTransformer);
        assertFalse(p1.expr().<Boolean>matchExpression().literal(l -> (Boolean) l.value()).orElse(null));
        var p2 = (UnaryPredicate) p.accept(new NothingTransformer());
        assertEquals(p, p2);
    }

    @Test
    void visitAndPredicate() {
        var lp = col("c1").in(1, 2);
        var rp = col("c2").eq(10);
        var and = lp.and(rp);
        var rowTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitRowExpr(RowExpr v) {
                return row(3, 4);
            }
        };
        var and1 = (AndPredicate) and.accept(rowTransformer);
        assertEquals(List.of(3, 4), and1.lhs().matchPredicate()
            .in(p -> p.rhs().matchValueSet()
                .row(r -> r.items().stream().map(i -> i.matchExpression()
                        .literal(l -> l.value())
                        .orElse(null)
                    ).toList()
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var litTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                var num = (Integer) l.value();
                if (num == 10) {
                    return lit(100);
                }
                return super.visitLiteralExpr(l);
            }
        };
        var and2 = (AndPredicate) and.accept(litTransformer);
        assertEquals(100, and2.rhs().matchPredicate()
            .comparison(cmp -> cmp.rhs().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null)
        );
        var and3 = (AndPredicate) and.accept(new NothingTransformer());
        assertEquals(and, and3);
    }

    @Test
    void visitOrPredicate() {
        var lp = col("c1").in(1, 2);
        var rp = col("c2").eq(10);
        var or = lp.or(rp);
        var rowTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitRowExpr(RowExpr v) {
                return row(3, 4);
            }
        };
        var or1 = (OrPredicate) or.accept(rowTransformer);
        assertEquals(List.of(3, 4), or1.lhs().matchPredicate()
            .in(p -> p.rhs().matchValueSet()
                .row(r -> r.items().stream().map(i -> i.matchExpression()
                        .literal(l -> l.value())
                        .orElse(null)
                    ).toList()
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var litTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                var num = (Integer) l.value();
                if (num == 10) {
                    return lit(100);
                }
                return super.visitLiteralExpr(l);
            }
        };
        var or2 = (OrPredicate) or.accept(litTransformer);
        assertEquals(100, or2.rhs().matchPredicate()
            .comparison(cmp -> cmp.rhs().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null)
        );
        var or3 = (OrPredicate) or.accept(new NothingTransformer());
        assertEquals(or, or3);
    }

    @Test
    void visitSelectQuery() {
        var query = select(lit(1))
            .from(tbl("t1"))
            .limit(1L)
            .build();
        var tableTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitTable(Table t) {
                return tbl("t2");
            }
        };
        var query1 = (SelectQuery) query.accept(tableTransformer);
        assertEquals("t2", query1.from().matchTableRef().table(t -> t.name()).orElse(null));
        var limitTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLimitOffset(LimitOffset l) {
                return LimitOffset.limit(2L);
            }
        };
        var query2 = (SelectQuery) query.accept(limitTransformer);
        assertInstanceOf(LiteralExpr.class, query2.limitOffset().limit());
        assertEquals(2L, ((LiteralExpr) query2.limitOffset().limit()).value());
    }

    @Test
    void visitCompositeQuery() {
        var query1 = select(lit(1)).build();
        var query2 = select(lit(2)).build();
        var cq = query1.union(query2).orderBy(order("c1")).limit(1L);
        // change one of the queries
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var cq1 = (CompositeQuery) cq.accept(columnTransformer);
        assertEquals("c11", cq1.orderBy().items().getFirst().expr().matchExpression().column(c -> c.name()).orElse(null));
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit(l.value() instanceof Integer i ? i + 1 : (Long) l.value() + 1);
            }
        };
        var cq2 = (CompositeQuery) cq.accept(literalTransformer);
        assertEquals(2, cq2.terms().getFirst().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var limitTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLimitOffset(LimitOffset l) {
                return LimitOffset.limit(2L);
            }
        };
        var cq3 = (CompositeQuery) cq.accept(limitTransformer);
        assertInstanceOf(LiteralExpr.class, cq3.limitOffset().limit());
        assertEquals(2L, ((LiteralExpr) cq3.limitOffset().limit()).value());
        var cq4 = (CompositeQuery) cq.accept(new NothingTransformer());
        assertEquals(cq, cq4);
    }

    @Test
    void visitWithQuery() {
        var cte = cte("cte", select(col("c1")).build());
        var body = select(col("c2")).build();
        var with = with(cte).body(body);
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var with1 = (WithQuery) with.accept(columnTransformer);
        assertEquals("c11", with1.ctes().getFirst().body().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .column(c -> c.name())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        assertEquals("c21", with1.body().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .column(c -> c.name())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var with2 = (WithQuery) with.accept(new NothingTransformer());
        assertEquals(with, with2);
    }

    @Test
    void visitWhenThen() {
        var whenThen = when(col("c1").eq(1)).then(2);
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var whenThen1 = (WhenThen) whenThen.accept(columnTransformer);
        assertEquals("c11", whenThen1.when().matchPredicate()
            .comparison(cmp -> cmp.lhs().matchExpression()
                .column(c -> c.name())
                .orElse(null)
            )
            .orElse(null)
        );
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                var num = (Integer) l.value();
                if (num == 2) {
                    return lit(3);
                }
                return l;
            }
        };
        var whenThen2 = (WhenThen) whenThen.accept(literalTransformer);
        assertEquals(3, whenThen2.then().matchExpression().literal(l -> l.value()).orElse(null));
        var whenThen3 = (WhenThen) whenThen.accept(new NothingTransformer());
        assertEquals(whenThen, whenThen3);
    }

    @Test
    void visitCte() {
    }

    @Test
    void visitWindowDef() {
        var window = window("w", partition(col("c1")));
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var window1 = (WindowDef) window.accept(columnTransformer);
        assertEquals("c11", window1.spec().partitionBy().items().getFirst().matchExpression().column(c -> c.name()).orElse(null));
        var window2 = (WindowDef) window.accept(new NothingTransformer());
        assertEquals(window, window2);
    }

    @Test
    void visitOverRef() {
    }

    @Test
    void visitOverDef() {
        var over = over(partition(col("p1")), orderBy(order(lit(1))), rows(currentRow()));
        var columnTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitColumnExpr(ColumnExpr c) {
                return col(c.name() + 1);
            }
        };
        var over1 = (OverSpec) over.accept(columnTransformer);
        assertEquals("p11", over1.matchOverSpec()
            .def(d -> d.partitionBy().items().getFirst().matchExpression()
                .column(c -> c.name())
                .orElse(null)
            )
            .orElse(null)
        );
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit((Integer) l.value() + 1);
            }
        };
        var over2 = (OverSpec) over.accept(literalTransformer);
        assertEquals(2, over2.matchOverSpec()
            .def(d -> d.orderBy().items().getFirst().expr().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null)
        );
        var frameTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitFrameSingle(FrameSpec.Single f) {
                return range(currentRow());
            }
        };
        var over3 = (OverSpec) over.accept(frameTransformer);
        assertEquals(FrameSpec.Unit.RANGE, over3.matchOverSpec()
            .def(d -> d.frame().unit())
            .orElse(null)
        );
        var baseOver = over("w", orderBy(order(lit(1))));
        var baseOver1 = (OverSpec) baseOver.accept(literalTransformer);
        assertEquals(2, baseOver1.matchOverSpec()
            .def(d -> d.orderBy().items().getFirst().expr().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void visitPartitionBy() {
    }

    @Test
    void visitFrameSingle() {
        var frame = rows(currentRow());
        var boundTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitBoundCurrentRow(BoundSpec.CurrentRow b) {
                return unboundedFollowing();
            }
        };
        var frame1 = (FrameSpec) frame.accept(boundTransformer);
        assertInstanceOf(BoundSpec.UnboundedFollowing.class, frame1.matchFrameSpec().single(s -> s.bound()).orElse(null));
    }

    @Test
    void visitFrameBetween() {
        var frame = rows(currentRow(), unboundedFollowing());
        var currentRowTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitBoundCurrentRow(BoundSpec.CurrentRow b) {
                return preceding(1);
            }
        };
        var frame1 = (FrameSpec) frame.accept(currentRowTransformer);
        assertEquals(1, frame1.matchFrameSpec()
            .between(b -> b.start().matchBoundSpec()
                .preceding(p -> p.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null)
        );
        var unboundedFollowingTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitBoundUnboundedFollowing(BoundSpec.UnboundedFollowing b) {
                return unboundedPreceding();
            }
        };
        var frame2 = (FrameSpec) frame.accept(unboundedFollowingTransformer);
        assertInstanceOf(BoundSpec.UnboundedPreceding.class, frame2.matchFrameSpec()
            .between(b -> b.end())
            .orElse(null)
        );
    }

    @Test
    void visitBoundUnboundedPreceding() {
    }

    @Test
    void visitBoundPreceding() {
        var bound = preceding(1);
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit((Integer) l.value() + 1);
            }
        };
        var bound1 = (BoundSpec) bound.accept(literalTransformer);
        assertEquals(2, bound1.matchBoundSpec()
            .preceding(p -> p.expr().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void visitBoundCurrentRow() {
    }

    @Test
    void visitBoundFollowing() {
        var bound = following(1);
        var literalTransformer = new RecursiveNodeTransformer() {
            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                return lit((Integer) l.value() + 1);
            }
        };
        var bound1 = (BoundSpec) bound.accept(literalTransformer);
        assertEquals(2, bound1.matchBoundSpec()
            .following(p -> p.expr().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null)
        );
    }

    @Test
    void visitBoundUnboundedFollowing() {
    }

    @Test
    void visitAnonymousParamExpr() {
        var p = param();
        var t = new RecursiveNodeTransformer() {
        };
        var p2 = p.accept(t);
        assertSame(p, p2);
    }

    @Test
    void visitOrdinalParamExpr() {
        var p = param(1);
        var t = new RecursiveNodeTransformer() {
        };
        var p2 = p.accept(t);
        assertSame(p, p2);
    }

    @Test
    void visitAddArithmeticExpr() {
        var e = lit(1).add(lit(2));
        // this transformer is used to apply transformation on literals
        // inside the expression. This way we can get full coverage.
        var increaseTransformer = new RecursiveNodeTransformer() {
            public int expected = 0;

            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if (l.value().equals(expected)) {
                    return lit(Literals.add(l, lit(1)));
                }
                return l;
            }
        };
        increaseTransformer.expected = 1; // transform only first literal
        e = (AddArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 2; // transform only second literal
        e = (AddArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 3; // do not transform any literal
        e = (AddArithmeticExpr) e.accept(increaseTransformer);
        var t = new RecursiveNodeTransformer() {
            @Override
            public Node visitAddArithmeticExpr(AddArithmeticExpr expr) {
                var r = Literals.add(expr.lhs(), expr.rhs());
                if (r != null) {
                    return lit(r.longValue());
                }
                return expr;
            }
        };
        var l = e.accept(t);
        assertInstanceOf(LiteralExpr.class, l);
        assertEquals(5L, ((LiteralExpr) l).value());
    }

    @Test
    void visitSubArithmeticExpr() {
        var e = lit(5).sub(lit(2));
        // this transformer is used to apply transformation on literals
        // inside the expression. This way we can get full coverage.
        var increaseTransformer = new RecursiveNodeTransformer() {
            public int expected = 0;

            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if (l.value().equals(expected)) {
                    return lit(Literals.add(l, lit(1)));
                }
                return l;
            }
        };
        increaseTransformer.expected = 5; // transform only first literal
        e = (SubArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 2; // transform only second literal
        e = (SubArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 0; // do not transform any literal
        e = (SubArithmeticExpr) e.accept(increaseTransformer);
        var t = new RecursiveNodeTransformer() {
            @Override
            public Node visitSubArithmeticExpr(SubArithmeticExpr expr) {
                var r = Literals.sub(expr.lhs(), expr.rhs());
                if (r != null) {
                    return lit(r.longValue());
                }
                return expr;
            }
        };
        var l = e.accept(t);
        assertInstanceOf(LiteralExpr.class, l);
        assertEquals(3L, ((LiteralExpr) l).value());
    }

    @Test
    void visitMulArithmeticExpr() {
        var e = lit(5).mul(lit(2));
        // this transformer is used to apply transformation on literals
        // inside the expression. This way we can get full coverage.
        var increaseTransformer = new RecursiveNodeTransformer() {
            public int expected = 0;

            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if (l.value().equals(expected)) {
                    return lit(Literals.add(l, lit(1)));
                }
                return l;
            }
        };
        increaseTransformer.expected = 5; // transform only first literal
        e = (MulArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 2; // transform only second literal
        e = (MulArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 0; // do not transform any literal
        e = (MulArithmeticExpr) e.accept(increaseTransformer);
        var t = new RecursiveNodeTransformer() {
            @Override
            public Node visitMulArithmeticExpr(MulArithmeticExpr expr) {
                var r = Literals.mul(expr.lhs(), expr.rhs());
                if (r != null) {
                    return lit(r.longValue());
                }
                return expr;
            }
        };
        var l = e.accept(t);
        assertInstanceOf(LiteralExpr.class, l);
        assertEquals(18L, ((LiteralExpr) l).value());
    }

    @Test
    void visitDivArithmeticExpr() {
        var e = lit(5).div(lit(1));
        // this transformer is used to apply transformation on literals
        // inside the expression. This way we can get full coverage.
        var increaseTransformer = new RecursiveNodeTransformer() {
            public int expected = 0;

            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if (l.value().equals(expected)) {
                    return lit(Literals.add(l, lit(1)));
                }
                return l;
            }
        };
        increaseTransformer.expected = 5; // transform only first literal
        e = (DivArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 1; // transform only second literal
        e = (DivArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 0; // do not transform any literal
        e = (DivArithmeticExpr) e.accept(increaseTransformer);
        var t = new RecursiveNodeTransformer() {
            @Override
            public Node visitDivArithmeticExpr(DivArithmeticExpr expr) {
                var r = Literals.div(expr.lhs(), expr.rhs());
                if (r != null) {
                    return lit(r.longValue());
                }
                return expr;
            }
        };
        var l = e.accept(t);
        assertInstanceOf(LiteralExpr.class, l);
        assertEquals(3L, ((LiteralExpr) l).value());
    }

    @Test
    void visitModArithmeticExpr() {
        var e = lit(6).mod(lit(2));
        // this transformer is used to apply transformation on literals
        // inside the expression. This way we can get full coverage.
        var increaseTransformer = new RecursiveNodeTransformer() {
            public int expected = 0;

            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if (l.value().equals(expected)) {
                    return lit(Literals.add(l, lit(1)));
                }
                return l;
            }
        };
        increaseTransformer.expected = 6; // transform only first literal
        e = (ModArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 2; // transform only second literal
        e = (ModArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 0; // do not transform any literal
        e = (ModArithmeticExpr) e.accept(increaseTransformer);
        var t = new RecursiveNodeTransformer() {
            @Override
            public Node visitModArithmeticExpr(ModArithmeticExpr expr) {
                var r = Literals.mod(expr.lhs(), expr.rhs());
                if (r != null) {
                    return lit(r.longValue());
                }
                return expr;
            }
        };
        var l = e.accept(t);
        assertInstanceOf(LiteralExpr.class, l);
        assertEquals(1L, ((LiteralExpr) l).value());
    }

    @Test
    void visitNegativeArithmeticExpr() {
        var e = lit(6).neg();
        // this transformer is used to apply transformation on literals
        // inside the expression. This way we can get full coverage.
        var increaseTransformer = new RecursiveNodeTransformer() {
            public int expected = 0;

            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if (l.value().equals(expected)) {
                    return lit(Literals.add(l, lit(1)));
                }
                return l;
            }
        };
        increaseTransformer.expected = 6; // transform only first literal
        e = (NegativeArithmeticExpr) e.accept(increaseTransformer);
        increaseTransformer.expected = 0; // do not transform any literal
        e = (NegativeArithmeticExpr) e.accept(increaseTransformer);
        var t = new RecursiveNodeTransformer() {
            @Override
            public Node visitNegativeArithmeticExpr(NegativeArithmeticExpr expr) {
                var r = Literals.asNumber(expr.expr());
                if (r != null) {
                    return lit(-r.longValue());
                }
                return expr;
            }
        };
        var l = e.accept(t);
        assertInstanceOf(LiteralExpr.class, l);
        assertEquals(-7L, ((LiteralExpr) l).value());
    }

    @Test
    void visitPowerArithmeticExpr() {
        var e = lit(2).pow(lit(3));
        var increaseTransformer = new RecursiveNodeTransformer() {
            public int expected = 0;

            @Override
            public Node visitLiteralExpr(LiteralExpr l) {
                if (l.value().equals(expected)) {
                    return lit(Literals.add(l, lit(1)));
                }
                return l;
            }
        };

        increaseTransformer.expected = 2; // transform only base literal
        e = (PowerArithmeticExpr) e.accept(increaseTransformer);
        assertEquals(3L, ((LiteralExpr) e.lhs()).value());
        assertEquals(3, ((LiteralExpr) e.rhs()).value());

        increaseTransformer.expected = 3; // transform exponent literal
        e = (PowerArithmeticExpr) e.accept(increaseTransformer);
        assertEquals(4L, ((LiteralExpr) e.rhs()).value());

        increaseTransformer.expected = 0; // do not transform any literal
        var unchanged = e.accept(increaseTransformer);
        assertSame(e, unchanged);
    }
}
