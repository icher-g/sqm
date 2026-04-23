package io.sqm.core.internal;

import io.sqm.core.ComparisonPredicate;
import io.sqm.core.Expression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.expr;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComparisonPredicateImplTest {

    @Test
    void rejectsRowValuesOnRightHandSide() {
        var row = Expression.row(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> col("c").eq(row));
    }

    @Test
    @DisplayName("allows scalar subquery on right-hand side")
    void allowsScalarSubqueryOnRightHandSide() {
        assertDoesNotThrow(() -> {
            ComparisonPredicate ignored = col("c").eq(
                expr(select(lit(1)).build())
            );
        });
    }
}
