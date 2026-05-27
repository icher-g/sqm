package io.sqm.core.walk;

import io.sqm.core.*;
import io.sqm.core.transform.RecursiveNodeTransformer;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonTableVisitorTransformerTest {
    @Test
    void recursiveVisitorTraversesJsonTableColumnsAndBehaviors() {
        var table = sampleTable();
        var collector = new Collector();

        table.accept(collector);

        assertTrue(collector.visited.contains("JsonTableRef"));
        assertTrue(collector.visited.contains("JsonTableScalarColumn"));
        assertTrue(collector.visited.contains("JsonTableOrdinalityColumn"));
        assertTrue(collector.visited.contains("JsonTableExistsColumn"));
        assertTrue(collector.visited.contains("JsonTableNestedPathColumn"));
        assertTrue(collector.visited.contains("JsonTableBehavior"));
        assertTrue(collector.visited.contains("ColumnExpr"));
        assertTrue(collector.visited.contains("LiteralExpr"));
        assertTrue(collector.visited.contains("TypeName"));
    }

    @Test
    void recursiveTransformerReturnsSameInstanceWhenUnchangedAndNewNodesWhenChanged() {
        var table = sampleTable();

        assertSame(table, new NoopTransformer().transform(table));

        var transformed = assertInstanceOf(JsonTableRef.class, new RenamePayloadTransformer().transform(table));
        assertNotSame(table, transformed);
        assertEquals("payload2", transformed.json().matchExpression().column(c -> c.name().value()).orElse("missing"));

        var scalar = assertInstanceOf(JsonTableScalarColumn.class, transformed.columns().getFirst());
        assertEquals("fallback2", scalar.onError().defaultExpression().matchExpression().literal(l -> l.value().toString()).orElse("missing"));

        var nested = assertInstanceOf(JsonTableNestedPathColumn.class, transformed.columns().get(3));
        var nestedScalar = assertInstanceOf(JsonTableScalarColumn.class, nested.columns().getFirst());
        assertEquals("child2", nestedScalar.name().value());
    }

    private static JsonTableRef sampleTable() {
        return jsonTable(
            col("payload"),
            jsonPath("$.items[*]"),
            jsonScalar(
                id("id"),
                type("NUMBER"),
                jsonPath("$.id"),
                JsonTableScalarColumn.Wrapper.DEFAULT,
                jsonBehavior(JsonTableBehavior.Kind.NULL),
                jsonBehavior(JsonTableBehavior.Kind.DEFAULT, lit("fallback"))
            ),
            jsonOrdinality("ord"),
            jsonExists(id("present"), type("BOOLEAN"), jsonPath("$.present"), jsonBehavior(JsonTableBehavior.Kind.ERROR)),
            jsonNested(jsonPath("$.children[*]"), jsonScalar("child", type("NUMBER"), jsonPath("$.id")))
        ).as("jt");
    }

    private static final class Collector extends RecursiveNodeVisitor<Void> {
        final Set<String> visited = new HashSet<>();

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitJsonTableRef(JsonTableRef t) {
            visited.add("JsonTableRef");
            return super.visitJsonTableRef(t);
        }

        @Override
        public Void visitJsonTableScalarColumn(JsonTableScalarColumn column) {
            visited.add("JsonTableScalarColumn");
            return super.visitJsonTableScalarColumn(column);
        }

        @Override
        public Void visitJsonTableOrdinalityColumn(JsonTableOrdinalityColumn column) {
            visited.add("JsonTableOrdinalityColumn");
            return super.visitJsonTableOrdinalityColumn(column);
        }

        @Override
        public Void visitJsonTableExistsColumn(JsonTableExistsColumn column) {
            visited.add("JsonTableExistsColumn");
            return super.visitJsonTableExistsColumn(column);
        }

        @Override
        public Void visitJsonTableNestedPathColumn(JsonTableNestedPathColumn column) {
            visited.add("JsonTableNestedPathColumn");
            return super.visitJsonTableNestedPathColumn(column);
        }

        @Override
        public Void visitJsonTableBehavior(JsonTableBehavior behavior) {
            visited.add("JsonTableBehavior");
            return super.visitJsonTableBehavior(behavior);
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
        public Void visitTypeName(TypeName t) {
            visited.add("TypeName");
            return super.visitTypeName(t);
        }
    }

    private static final class RenamePayloadTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitColumnExpr(ColumnExpr c) {
            if (c.tableAlias() == null && "payload".equals(c.name().value())) {
                return col("payload2");
            }
            if (c.tableAlias() == null && "child".equals(c.name().value())) {
                return col("child2");
            }
            return c;
        }

        @Override
        public Node visitLiteralExpr(LiteralExpr l) {
            if ("fallback".equals(l.value())) {
                return lit("fallback2");
            }
            return l;
        }

        @Override
        public Node visitJsonTableScalarColumn(JsonTableScalarColumn column) {
            var transformed = (JsonTableScalarColumn) super.visitJsonTableScalarColumn(column);
            if ("child".equals(transformed.name().value())) {
                return JsonTableScalarColumn.of(
                    id("child2"),
                    transformed.type(),
                    transformed.path(),
                    transformed.wrapper(),
                    transformed.onEmpty(),
                    transformed.onError()
                );
            }
            return transformed;
        }
    }

    private static final class NoopTransformer extends RecursiveNodeTransformer {
    }
}
