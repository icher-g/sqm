package io.sqm.core.walk;

import io.sqm.core.*;
import io.sqm.core.transform.RecursiveNodeTransformer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecursiveNodeVisitor - FunctionTable and Lateral Tests")
class RecursiveNodeVisitorTest {

    @Test
    @DisplayName("Visit FunctionTable")
    void visitFunctionTable() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = func.asTable().as("t");

        var collector = new NodeCollector();
        table.accept(collector);

        assertTrue(collector.visited.contains("FunctionTable"));
        assertTrue(collector.visited.contains("FunctionExpr"));
        assertTrue(collector.visited.contains("LiteralExpr"));
    }

    @Test
    @DisplayName("Visit Lateral")
    void visitLateral() {
        var subquery = select(col("*")).from(tbl("users")).build();
        var lateral = tbl(subquery).as("sub").lateral();

        var collector = new NodeCollector();
        lateral.accept(collector);

        assertTrue(collector.visited.contains("Lateral"));
        assertTrue(collector.visited.contains("QueryTable"));
        assertTrue(collector.visited.contains("SelectQuery"));
    }

    @Test
    @DisplayName("Visit nested Lateral with FunctionTable")
    void visitNestedLateralWithFunctionTable() {
        var func = func("unnest", arg(col("arr")));
        var lateral = func.asTable().as("t").lateral();

        var collector = new NodeCollector();
        lateral.accept(collector);

        assertTrue(collector.visited.contains("Lateral"));
        assertTrue(collector.visited.contains("FunctionTable"));
        assertTrue(collector.visited.contains("FunctionExpr"));
        assertTrue(collector.visited.contains("ColumnExpr"));
    }

    @Test
    @DisplayName("Visit query with FunctionTable in FROM")
    void visitQueryWithFunctionTableInFrom() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var query = select(col("num")).from(func.asTable().as("series")).build();

        var collector = new NodeCollector();
        query.accept(collector);

        assertTrue(collector.visited.contains("SelectQuery"));
        assertTrue(collector.visited.contains("FunctionTable"));
        assertTrue(collector.visited.contains("FunctionExpr"));
    }

    @Test
    @DisplayName("Visit query with Lateral JOIN")
    void visitQueryWithLateralJoin() {
        var func = func("unnest", arg(col("t", "arr")));
        var query = select(col("*"))
            .from(tbl("t"))
            .join(inner(func.asTable().as("u").lateral())
                .on(col("t", "id").eq(col("u", "id"))))
            .build();

        var collector = new NodeCollector();
        query.accept(collector);

        assertTrue(collector.visited.contains("SelectQuery"));
        assertTrue(collector.visited.contains("Lateral"));
        assertTrue(collector.visited.contains("FunctionTable"));
        assertTrue(collector.visited.contains("OnJoin"));
    }

    @Test
    @DisplayName("Transform FunctionTable")
    void transformFunctionTable() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = func.asTable().as("t");

        var transformer = new RenameTableTransformer();
        var transformed = transformer.transform(table);

        assertInstanceOf(FunctionTable.class, transformed);
        assertEquals("renamed", ((FunctionTable) transformed).alias().value());
    }

    @Test
    @DisplayName("Transform Lateral")
    void transformLateral() {
        var subquery = select(col("*")).from(tbl("users")).build();
        var lateral = tbl(subquery).as("sub").lateral();

        var transformer = new RenameTableTransformer();
        var transformed = transformer.transform(lateral);

        assertInstanceOf(Lateral.class, transformed);
        var inner = ((Lateral) transformed).inner();
        assertInstanceOf(QueryTable.class, inner);
        assertEquals("renamed", ((QueryTable) inner).alias().value());
    }

    @Test
    @DisplayName("Transform FunctionTable function arguments")
    void transformFunctionTableArguments() {
        var func = func("generate_series", arg(lit(1)), arg(lit(10)));
        var table = func.asTable().as("t");

        var transformer = new IncrementLiteralsTransformer();
        var transformed = transformer.transform(table);

        assertInstanceOf(FunctionTable.class, transformed);
        var transformedFunc = ((FunctionTable) transformed).function();
        
        // Also check if alias was preserved
        assertEquals("t", ((FunctionTable) transformed).alias().value());
        
        var firstArg = (FunctionExpr.Arg.ExprArg) transformedFunc.args().getFirst();
        assertEquals(2, ((LiteralExpr) firstArg.expr()).value());
    }

    private static class NodeCollector extends RecursiveNodeVisitor<Void> {
        final java.util.Set<String> visited = new java.util.HashSet<>();

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitFunctionTable(FunctionTable t) {
            visited.add("FunctionTable");
            return super.visitFunctionTable(t);
        }

        @Override
        public Void visitLateral(Lateral i) {
            visited.add("Lateral");
            return super.visitLateral(i);
        }

        @Override
        public Void visitQueryTable(QueryTable t) {
            visited.add("QueryTable");
            return super.visitQueryTable(t);
        }

        @Override
        public Void visitSelectQuery(SelectQuery q) {
            visited.add("SelectQuery");
            return super.visitSelectQuery(q);
        }

        @Override
        public Void visitFunctionExpr(FunctionExpr f) {
            visited.add("FunctionExpr");
            return super.visitFunctionExpr(f);
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            visited.add("ColumnExpr");
            return super.visitColumnExpr(c);
        }

        @Override
        public Void visitLiteralExpr(LiteralExpr l) {
            visited.add("LiteralExpr");
            return super.visitLiteralExpr(l);
        }

        @Override
        public Void visitOnJoin(OnJoin j) {
            visited.add("OnJoin");
            return super.visitOnJoin(j);
        }
    }

    private static class RenameTableTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitFunctionTable(FunctionTable t) {
            var transformed = (FunctionTable) super.visitFunctionTable(t);
            return transformed.as("renamed");
        }

        @Override
        public Node visitQueryTable(QueryTable t) {
            return t.as("renamed");
        }
    }

    private static class IncrementLiteralsTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitLiteralExpr(LiteralExpr l) {
            if (l.value() instanceof Long longVal) {
                return LiteralExpr.of(longVal + 1);
            } else if (l.value() instanceof Integer intVal) {
                return LiteralExpr.of(intVal + 1);
            }
            return l;
        }
    }
}
