package io.sqm.codegen.generated;

import javax.annotation.processing.Generated;
import io.sqm.core.Query;
import java.util.Set;
import static io.sqm.dsl.Dsl.*;

/**
 * Generated from SQL files located in analytics.
 * Dialect: ANSI.
 * Source SQL paths:
 * - analytics/ranked.sql
 */
@Generated(
    value = "io.sqm.codegen.SqlFileCodeGenerator",
    comments = "dialect=ANSI; sqlFolder=analytics; sqlFiles=analytics/ranked.sql"
)
public final class AnalyticsQueries {

    private AnalyticsQueries() {
    }

    /**
     * SQL source: analytics/ranked.sql
     *
     * @return query model for this SQL source.
     */
    public static Query ranked() {
        return select(
          func("row_number").over(over(partition(col("dept")), orderBy(order(col("salary")).desc()))).as("rn")
        )
        .from(tbl("employees"));
    }

    /**
     * Returns named parameters referenced by analytics/ranked.sql.
     *
     * @return immutable set of named parameter identifiers.
     */
    public static Set<String> rankedParams() {
        return Set.of();
    }

}
