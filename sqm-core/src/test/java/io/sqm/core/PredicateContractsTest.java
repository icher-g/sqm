package io.sqm.core;

import org.junit.jupiter.api.Test;

import static io.sqm.core.Expression.*;
import static org.junit.jupiter.api.Assertions.*;

public class PredicateContractsTest {

    @Test
    void any_all_predicate() {
        var p = AnyAllPredicate.of(
            ColumnExpr.of("c1"),
            ComparisonOperator.GT,
            Query.select(literal(1)),
            Quantifier.ALL);

        assertEquals("c1", p.lhs().matchExpression().column(c -> c.name()).orElse(null));
        assertEquals(ComparisonOperator.GT, p.operator());
        assertEquals(1, (Integer) p.subquery().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null))
                .orElse(null))
            .orElse(null));
    }

    @Test
    void between_predicate() {
        var p = BetweenPredicate.of(
            column("c"),
            literal(1),
            literal(10),
            false,
            false);

        assertEquals("c", p.value().matchExpression().column(l -> l.name()).orElse(null));
        assertEquals(1, p.lower().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals(10, p.upper().matchExpression().literal(l -> l.value()).orElse(null));
        assertFalse(p.symmetric());
    }

    @Test
    void comparison_predicate() {
        var p = ComparisonPredicate.of(column("c"), ComparisonOperator.EQ, literal(1));
        assertEquals("c", p.lhs().matchExpression().column(l -> l.name()).orElse(null));
        assertEquals(ComparisonOperator.EQ, p.operator());
        assertEquals(1, p.rhs().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void and_predicate() {
        var p = AndPredicate.of(
            ComparisonPredicate.of(column("c1"), ComparisonOperator.EQ, literal(1)),
            ComparisonPredicate.of(column("c2"), ComparisonOperator.GT, literal(2))
        );

        assertInstanceOf(ComparisonPredicate.class, p.lhs());
        assertInstanceOf(ComparisonPredicate.class, p.rhs());
        assertEquals("c1", p.lhs().matchPredicate()
            .comparison(c -> c.lhs().matchExpression()
                .column(l -> l.name())
                .orElse(null)
            )
            .orElse(null));
        assertEquals(ComparisonOperator.EQ, p.lhs().matchPredicate()
            .comparison(c -> c.operator())
            .orElse(null));
        assertEquals(1, p.lhs().matchPredicate()
            .comparison(c -> c.rhs().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null));
        assertEquals("c2", p.rhs().matchPredicate()
            .comparison(c -> c.lhs().matchExpression()
                .column(l -> l.name())
                .orElse(null)
            )
            .orElse(null));
        assertEquals(ComparisonOperator.GT, p.rhs().matchPredicate()
            .comparison(c -> c.operator())
            .orElse(null));
        assertEquals(2, p.rhs().matchPredicate()
            .comparison(c -> c.rhs().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null));
    }

    @Test
    void or_predicate() {
        var p = OrPredicate.of(
            ComparisonPredicate.of(column("c1"), ComparisonOperator.EQ, literal(1)),
            ComparisonPredicate.of(column("c2"), ComparisonOperator.GT, literal(2))
        );

        assertInstanceOf(ComparisonPredicate.class, p.lhs());
        assertInstanceOf(ComparisonPredicate.class, p.rhs());
        assertEquals("c1", p.lhs().matchPredicate()
            .comparison(c -> c.lhs().matchExpression()
                .column(l -> l.name())
                .orElse(null)
            )
            .orElse(null));
        assertEquals(ComparisonOperator.EQ, p.lhs().matchPredicate()
            .comparison(c -> c.operator())
            .orElse(null));
        assertEquals(1, p.lhs().matchPredicate()
            .comparison(c -> c.rhs().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null));
        assertEquals("c2", p.rhs().matchPredicate()
            .comparison(c -> c.lhs().matchExpression()
                .column(l -> l.name())
                .orElse(null)
            )
            .orElse(null));
        assertEquals(ComparisonOperator.GT, p.rhs().matchPredicate()
            .comparison(c -> c.operator())
            .orElse(null));
        assertEquals(2, p.rhs().matchPredicate()
            .comparison(c -> c.rhs().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null));
    }

    @Test
    void exists_predicate() {
        var p = ExistsPredicate.of(Query.select(literal(1)), false);

        assertEquals(1, (Integer) p.subquery().matchQuery()
            .select(s -> s.items().getFirst().matchSelectItem()
                .expr(e -> e.expr().matchExpression()
                    .literal(l -> l.value())
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null));
        assertFalse(p.negated());
    }

    @Test
    void in_predicate() {
        var p = InPredicate.of(column("c"), row(1), true);

        assertEquals("c", p.lhs().matchExpression().column(l -> l.name()).orElse(null));
        assertEquals(1, p.rhs().matchValueSet().row(r -> r.items().size()).orElse(0));
        assertEquals(1, p.rhs().matchValueSet()
            .row(r -> r.items().getFirst().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null));
        assertTrue(p.negated());
    }

    @Test
    void is_null_predicate() {
        var p = IsNullPredicate.of(column("c"), true);

        assertEquals("c", p.expr().matchExpression().column(l -> l.name()).orElse(null));
        assertTrue(p.negated());
    }

    @Test
    void like_predicate() {
        var p = LikePredicate.of(column("c"), literal("%c%"), literal("\\"), true);

        assertEquals("c", p.value().matchExpression().column(l -> l.name()).orElse(null));
        assertEquals("%c%", p.pattern().matchExpression().literal(l -> l.value()).orElse(null));
        assertEquals("\\", p.escape().matchExpression().literal(l -> l.value()).orElse(null));
        assertTrue(p.negated());
    }

    @Test
    void not_predicate() {
        var p = NotPredicate.of(ComparisonPredicate.of(column("c1"), ComparisonOperator.EQ, literal(1)));

        assertInstanceOf(ComparisonPredicate.class, p.inner());
        assertEquals("c1", p.inner().matchPredicate()
            .comparison(c -> c.lhs().matchExpression()
                .column(col -> col.name())
                .orElse(null)
            )
            .orElse(null));
        assertEquals(ComparisonOperator.EQ, p.inner().matchPredicate()
            .comparison(c -> c.operator())
            .orElse(null));
        assertEquals(1, p.inner().matchPredicate()
            .comparison(c -> c.rhs().matchExpression()
                .literal(l -> l.value())
                .orElse(null)
            )
            .orElse(null));
    }

    @Test
    void unary_predicate() {
        var p = UnaryPredicate.of(literal(true));

        assertTrue((boolean) p.expr().matchExpression().literal(l -> l.value()).orElse(null));
    }
}
