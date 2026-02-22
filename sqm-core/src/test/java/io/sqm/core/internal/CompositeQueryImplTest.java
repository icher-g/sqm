package io.sqm.core.internal;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import io.sqm.core.SetOperator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeQueryImplTest {

    @Test
    void negative() {
        List<Query> terms = List.of(Query.select(Expression.literal(1)).build(), Query.select(Expression.literal(2)).build());
        var ops = List.of(SetOperator.EXCEPT, SetOperator.UNION);
        assertThrows(IllegalArgumentException.class, () -> CompositeQuery.of(terms, ops));
    }
}