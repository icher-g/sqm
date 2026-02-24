package io.sqm.examples;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Node;
import io.sqm.core.Query;
import io.sqm.core.transform.RecursiveNodeTransformer;

import static io.sqm.dsl.Dsl.*;

public class Transform_RenameColumnTransformer {
    public static void main(String[] args) {
        Query query = select(
                col("u", "user_name"),
                col("o", "status"),
                func("count", starArg()).as("cnt")
            )
            .from(tbl("orders").as("o"))
            .join(
                inner(tbl("users").as("u")).on(col("u", "id").eq(col("o", "user_id")))
            )
            .where(col("o", "status").in("A", "B"))
            .groupBy(group("u", "user_name"), group("o", "status"))
            .having(func("count", starArg()).gt(10))
            .build();

        var transformer = new Transform_RenameColumnTransformer.RenameColumnTransformer();
        var transformedQuery = (Query) query.accept(transformer);

        // print the u.id new column name used in join statement.
        var name = transformedQuery.matchQuery()
            .select(s -> s.joins().getFirst().matchJoin()
                .on(j -> j.on().matchPredicate()
                    .comparison(cmp -> cmp.lhs().matchExpression()
                        .column(c -> c.name())
                        .orElse(null)
                    )
                    .orElse(null)
                )
                .orElse(null)
            )
            .orElse(null);

        System.out.println(name);
    }

    public static class RenameColumnTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitColumnExpr(ColumnExpr c) {
            if ("u".equals(c.tableAlias().value()) && "id".equals(c.name().value())) {
                return col("u", "user_id");
            }
            return c;
        }
    }
}
