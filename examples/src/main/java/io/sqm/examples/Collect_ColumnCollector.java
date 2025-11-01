package io.sqm.examples;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Query;
import io.sqm.core.walk.RecursiveNodeVisitor;

import java.util.LinkedHashSet;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;

public final class Collect_ColumnCollector {
    public static void main(String[] args) {
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

        var collector = new QueryColumnCollector();
        q.accept(collector);

        for (var c : collector.getColumns()) {
            System.out.println(c);
        }
    }

    /**
     * Implement the query walker with the help of RecursiveNodeVisitor.
     */
    private static class QueryColumnCollector extends RecursiveNodeVisitor<Void> {

        private final Set<String> columns = new LinkedHashSet<>();

        @Override
        protected Void defaultResult() {
            return null;
        }

        public Set<String> getColumns() {
            return columns;
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            columns.add(c.tableAlias() == null ? c.name() : c.tableAlias() + "." + c.name());
            return super.visitColumnExpr(c);
        }
    }
}
