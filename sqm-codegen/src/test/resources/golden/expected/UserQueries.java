package io.sqm.codegen.generated;

import javax.annotation.processing.Generated;
import io.sqm.core.*;
import java.util.Set;
import static io.sqm.dsl.Dsl.*;

/**
 * Generated from SQL files located in user.
 * Dialect: ANSI.
 * Source SQL paths:
 * - user/a_find_by_id.sql
 * - user/z_list_active.sql
 */
@Generated(
    value = "io.sqm.codegen.SqlFileCodeGenerator",
    comments = "dialect=ANSI; sqlFolder=user; sqlFiles=user/a_find_by_id.sql,user/z_list_active.sql"
)
public final class UserQueries {

    private UserQueries() {
    }

    /**
     * SQL source: user/a_find_by_id.sql
     *
     * @return query model for this SQL source.
     */
    public static SelectQuery aFindById() {
        return select(
          star()
        )
        .from(tbl("users"))
        .where(col("id").eq(param("id")).and(col("status").eq(param("status"))));
    }

    /**
     * Returns named parameters referenced by user/a_find_by_id.sql.
     *
     * @return immutable set of named parameter identifiers.
     */
    public static Set<String> aFindByIdParams() {
        return Set.of("id", "status");
    }

    /**
     * SQL source: user/z_list_active.sql
     *
     * @return query model for this SQL source.
     */
    public static SelectQuery zListActive() {
        return select(
          star()
        )
        .from(tbl("users"))
        .where(col("status").eq(param("status")));
    }

    /**
     * Returns named parameters referenced by user/z_list_active.sql.
     *
     * @return immutable set of named parameter identifiers.
     */
    public static Set<String> zListActiveParams() {
        return Set.of("status");
    }

}
