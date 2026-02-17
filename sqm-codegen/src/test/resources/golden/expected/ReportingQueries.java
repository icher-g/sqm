package io.sqm.codegen.generated;

import javax.annotation.processing.Generated;
import io.sqm.core.*;
import java.util.Set;
import static io.sqm.dsl.Dsl.*;

/**
 * Generated from SQL files located in reporting.
 * Dialect: ANSI.
 * Source SQL paths:
 * - reporting/kitchen_sink.sql
 */
@Generated(
    value = "io.sqm.codegen.SqlFileCodeGenerator",
    comments = "dialect=ANSI; sqlFolder=reporting; sqlFiles=reporting/kitchen_sink.sql"
)
public final class ReportingQueries {

    private ReportingQueries() {
    }

    /**
     * SQL source: reporting/kitchen_sink.sql
     *
     * @return query model for this SQL source.
     */
    public static SelectQuery kitchenSink() {
        return select(
          col("u", "id"),
          col("u", "org_id"),
          func("count", starArg()).as("total_orders"),
          func("sum", arg(col("o", "amount"))).as("total_amount"),
          func("row_number").over(over(partition(col("u", "org_id")), orderBy(order(col("o", "created_at")).desc()))).as("org_rank")
        )
        .from(tbl("users").as("u"))
        .join(
          left(tbl("orders").as("o")).on(col("o", "user_id").eq(col("u", "id")))
        )
        .where(col("u", "status").eq(param("status")).and(col("o", "created_at").gte(lit("2024-01-01"))).and(col("o", "created_at").lt(lit("2025-01-01"))).and(col("o", "kind").in(row(lit("A"), lit("B")))))
        .groupBy(group(col("u", "id")), group(col("u", "org_id")))
        .having(func("count", starArg()).gt(lit(1L)))
        .orderBy(order(func("sum", arg(col("o", "amount")))).desc().nullsLast(), order(col("u", "id")).asc())
        .limitOffset(limitOffset(lit(100L), lit(10L)))
        .lockFor(update(), ofTables("u", "o"), false, true);
    }

    /**
     * Returns named parameters referenced by reporting/kitchen_sink.sql.
     *
     * @return immutable set of named parameter identifiers.
     */
    public static Set<String> kitchenSinkParams() {
        return Set.of("status");
    }

}
