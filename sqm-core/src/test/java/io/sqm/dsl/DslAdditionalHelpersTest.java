package io.sqm.dsl;

import io.sqm.core.BoundSpec;
import io.sqm.core.DistinctSpec;
import io.sqm.core.LimitOffset;
import io.sqm.core.Nulls;
import io.sqm.core.OverSpec;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class DslAdditionalHelpersTest {

    @Test
    void orderOrdinalAndNullsHelpers() {
        var item = order(1).desc().nullsLast();
        assertEquals(1, item.ordinal());
        assertEquals(Nulls.LAST, item.nulls());

        var reset = item.nullsDefault();
        assertEquals(Nulls.DEFAULT, reset.nulls());
    }

    @Test
    void boundExpressionHelpers() {
        BoundSpec preceding = preceding(param("n"));
        BoundSpec following = following(param("m"));

        assertInstanceOf(BoundSpec.Preceding.class, preceding);
        assertInstanceOf(BoundSpec.Following.class, following);
    }

    @Test
    void overHelpersWithoutPartition() {
        var byOrder = over(orderBy(order(col("created_at")).desc()));
        assertInstanceOf(OverSpec.Def.class, byOrder);
        assertNotNull(byOrder.orderBy());

        var empty = over();
        assertInstanceOf(OverSpec.Def.class, empty);

        var baseOnly = overDef("w");
        assertInstanceOf(OverSpec.Def.class, baseOnly);
        assertEquals("w", baseOnly.baseWindow().value());

        var withExclude = over(orderBy(order(col("created_at"))), rows(preceding(1), currentRow()), excludeNoOthers());
        assertInstanceOf(OverSpec.Def.class, withExclude);
        assertEquals(OverSpec.Exclude.NO_OTHERS, withExclude.exclude());
    }

    @Test
    void distinctHelpers() {
        DistinctSpec plain = distinct();
        DistinctSpec onExpr = distinctOn(col("a"), col("b"));

        assertNotNull(plain);
        assertTrue(plain.items().isEmpty());
        assertEquals(2, onExpr.items().size());
    }

    @Test
    void limitOffsetHelpers() {
        LimitOffset pair = limitOffset(lit(10), lit(5));
        LimitOffset allOnly = limitAll();
        LimitOffset allWithOffset = limitAll(lit(3));

        assertNotNull(pair.limit());
        assertNotNull(pair.offset());
        assertTrue(allOnly.limitAll());
        assertTrue(allWithOffset.limitAll());
        assertNotNull(allWithOffset.offset());
    }
}
