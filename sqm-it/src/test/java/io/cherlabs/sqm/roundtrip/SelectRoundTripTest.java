package io.cherlabs.sqm.roundtrip;

import io.cherlabs.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static io.cherlabs.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectRoundTripTest {

    @Test
    void simple_select_from_where_group_having() {
        Query original = query()
            .select(col("u", "user_name"),
                col("o", "status"),
                func("count", star()).as("cnt"))
            .from(tbl("orders").as("o"))
            .join(inner(tbl("users").as("u"))
                .on(col("u", "id").eq(col("o", "user_id"))))
            .where(col("o", "status").in("A", "B"))
            .groupBy(group("u", "user_name"), group("o", "status"))
            .having(func("count", star()).gt(10));

        String sql = RoundTripTestUtil.renderAnsi(original);
        Query reparsed = RoundTripTestUtil.parse(sql);

        // Prefer model comparison if equals/hashCode are implemented well,
        // otherwise compare canonical JSON or re-render both and compare SQL.
        assertEquals(RoundTripTestUtil.canonicalJson(reparsed), RoundTripTestUtil.canonicalJson(original));
    }

    @Test
    void select_with_order_by_limit_offset() {
        Query original = query()
            .select(col("p", "id"), col("p", "name"))
            .from(tbl("products").as("p"))
            .orderBy(order(col("p", "name")).asc())
            .limit(10)
            .offset(20);

        String sql = RoundTripTestUtil.renderAnsi(original);
        Query reparsed = RoundTripTestUtil.parse(sql);

        assertEquals(RoundTripTestUtil.canonicalJson(reparsed), RoundTripTestUtil.canonicalJson(original));
    }
}
