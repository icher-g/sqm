package io.sqm.core.internal;

import io.sqm.core.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static io.sqm.dsl.Dsl.col;

class ComparisonPredicateImplTest {

    @Test
    void negative() {
        var row = Expression.row(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> col("c").eq(row));
    }
}
