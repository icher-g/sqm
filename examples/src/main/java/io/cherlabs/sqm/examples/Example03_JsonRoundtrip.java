package io.cherlabs.sqm.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;
import io.cherlabs.sqm.json.SqmMapperFactory;

import static io.cherlabs.sqm.dsl.Dsl.*;

public final class Example03_JsonRoundtrip {
    public static void main(String[] args) throws Exception {
        Query q = query()
            .select(
                col("u", "user_name"),
                col("o", "status"),
                func("count", star()).as("cnt")
            )
            .from(tbl("orders").as("o"))
            .join(
                inner(tbl("users").as("u"))
                    .on(col("u", "id").eq(col("o", "user_id")))
            )
            .where(col("o", "status").in("A", "B"))
            .groupBy(group("u", "user_name"), group("o", "status"))
            .having(func("count", star()).gt(10));

        ObjectMapper mapper = SqmMapperFactory.createDefault(); // ensure it registers all your MixIns

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(q);
        System.out.println("=== JSON ===");
        System.out.println(json);

        Query back = mapper.readValue(json, SelectQuery.class);
        System.out.println("Round-trip OK? " + (back != null));
    }
}
