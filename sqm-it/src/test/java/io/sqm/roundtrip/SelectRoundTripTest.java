package io.sqm.roundtrip;

import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectRoundTripTest {

    @Test
    void simple_select_from_where_group_having() {
        Query original =
            select(
                col("u", "user_name"),
                col("o", "status"),
                func("count", starArg()).as("cnt"))
            .from(tbl("orders").as("o"))
            .join(inner(tbl("users").as("u"))
                .on(col("u", "id").eq(col("o", "user_id"))))
            .where(col("o", "status").in("A", "B"))
            .groupBy(group("u", "user_name"), group("o", "status"))
            .having(func("count", starArg()).gt(10));

        String sql = RoundTripTestUtil.renderAnsi(original);
        Query reparsed = RoundTripTestUtil.parse(sql);

        String expected = RoundTripTestUtil.canonicalJson(original);
        String actual = RoundTripTestUtil.canonicalJson(reparsed);
        assertEquals(expected, actual);
    }

    @Test
    void select_with_order_by_limit_offset() {
        Query original =
            select(col("p", "id"), col("p", "name"))
            .from(tbl("products").as("p"))
            .orderBy(order(col("p", "name")).asc())
            .limit(10)
            .offset(20);

        String sql = RoundTripTestUtil.renderAnsi(original);
        Query reparsed = RoundTripTestUtil.parse(sql);

        assertEquals(RoundTripTestUtil.canonicalJson(reparsed), RoundTripTestUtil.canonicalJson(original));
    }
}
