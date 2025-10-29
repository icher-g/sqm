package io.sqm.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.json.SqmJsonMixins;

import static io.sqm.dsl.Dsl.*;

public final class Example03_JsonRoundtrip {
    public static void main(String[] args) throws Exception {
        Query q = select(
                col("u", "user_name").toSelectItem(),
                col("o", "status").toSelectItem(),
                func("count", starArg()).as("cnt")
            )
            .from(tbl("orders").as("o"))
            .join(
                inner(tbl("users").as("u"))
                    .on(col("u", "id").eq(col("o", "user_id")))
            )
            .where(col("o", "status").in("A", "B"))
            .groupBy(group("u", "user_name"), group("o", "status"))
            .having(func("count", starArg()).gt(10));

        ObjectMapper mapper = SqmJsonMixins.createDefault(); // ensure it registers all your MixIns

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(q);
        System.out.println("=== JSON ===");
        System.out.println(json);

        Query back = mapper.readValue(json, SelectQuery.class);
        System.out.println("Round-trip OK? " + (back != null));
    }
}
