package io.sqm.core.internal;

import io.sqm.core.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ComparisonPredicateImplTest {

    @Test
    void negative() {
        var row = Expression.row(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> Expression.column("c").eq(row));
    }
}