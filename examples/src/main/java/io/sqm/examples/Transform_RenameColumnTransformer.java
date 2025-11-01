package io.sqm.examples;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Node;
import io.sqm.core.Query;
import io.sqm.core.transform.RecursiveNodeTransformer;

import static io.sqm.dsl.Dsl.*;

public class Transform_RenameColumnTransformer {
    public static void main(String[] args) {
        Query query = select(
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

        var transformer = new Transform_RenameColumnTransformer.RenameColumnTransformer();
        var transformedQuery = (Query)query.accept(transformer);

        // print the u.id new column name used in join statement.
        transformedQuery.asSelect().flatMap(s -> s.joins().get(0).asOn().flatMap(j -> j.on().asComparison().flatMap(p -> p.lhs().asColumn()))).ifPresent(c -> System.out.println(c.name()));
    }

    public static class RenameColumnTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitColumnExpr(ColumnExpr c) {
            if ("u".equals(c.tableAlias()) && "id".equals(c.name())) {
                return ColumnExpr.of("u", "user_id");
            }
            return c;
        }
    }
}
