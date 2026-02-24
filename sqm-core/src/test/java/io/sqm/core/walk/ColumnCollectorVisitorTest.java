package io.sqm.core.walk;

import io.sqm.core.ColumnExpr;
import io.sqm.core.FunctionExpr;
import io.sqm.core.Identifier;
import io.sqm.core.QualifiedName;
import io.sqm.core.RowExpr;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Typical usage: override only {@code visitColumnExpr} and rely on recursion
 * to reach all leaves from a complex expression tree.
 */
public class ColumnCollectorVisitorTest {

    @Test
    void collectsColumns_fromNestedExpressions() {
        ColumnExpr colUid = ColumnExpr.of(Identifier.of("u"), Identifier.of("id"));
        ColumnExpr colName = ColumnExpr.of(null, Identifier.of("name"));

        FunctionExpr lower = FunctionExpr.of(QualifiedName.of("lower"), List.of(FunctionExpr.Arg.expr(colName)), null, null, null, null);
        RowExpr row = RowExpr.of(List.of(colUid, lower));

        ColumnCollector collector = new ColumnCollector();
        row.accept(collector);

        assertEquals(Set.of("u.id", "name"), collector.getColumns());
    }

    static class ColumnCollector extends RecursiveNodeVisitor<Void> {
        private final Set<String> cols = new LinkedHashSet<>();

        public Set<String> getColumns() {
            return cols;
        }

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            cols.add(c.tableAlias() == null ? c.name().value() : c.tableAlias().value() + "." + c.name().value());
            return super.visitColumnExpr(c); // continue recursion if there are nested nodes (usually none for columns)
        }
    }
}


