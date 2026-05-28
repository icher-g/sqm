package io.sqm.core;

import io.sqm.core.transform.IdentifierNormalizationTransformer;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class TableAccessModifierTest {
    @Test
    void factoriesCreateVersionPartitionAndSampleSpecs() {
        var asOfTimestamp = asOfTimestamp(lit(10));
        var asOfScn = asOfScn(lit(11));
        var fromTo = tableVersionFromTo(lit(1), lit(2));
        var between = tableVersionBetween(lit(3), lit(4));
        var containedIn = tableVersionContainedIn(lit(5), lit(6));
        var all = tableVersionAll();

        assertEquals(TableVersionSpec.TableVersionKind.AS_OF_TIMESTAMP, asOfTimestamp.kind());
        assertEquals(TableVersionSpec.TableVersionKind.AS_OF_SCN, asOfScn.kind());
        assertEquals(TableVersionSpec.TableVersionKind.FROM_TO, fromTo.kind());
        assertEquals(TableVersionSpec.TableVersionKind.BETWEEN, between.kind());
        assertEquals(TableVersionSpec.TableVersionKind.CONTAINED_IN, containedIn.kind());
        assertEquals(TableVersionSpec.TableVersionKind.ALL, all.kind());
        assertEquals(TablePartitionSpec.TablePartitionSpecKind.PARTITION, tablePartition("P1").kind());
        assertEquals(TablePartitionSpec.TablePartitionSpecKind.SUBPARTITION, subpartition("SP1").kind());
        assertEquals(TableSampleSpec.SampleUnit.PERCENT,
            tableSample(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.PERCENT, lit(10), lit(42)).unit());
    }

    @Test
    void factoriesValidateRequiredState() {
        assertThrows(NullPointerException.class, () -> TableVersionSpec.of(TableVersionSpec.TableVersionKind.AS_OF_SCN, null));
        assertThrows(NullPointerException.class, () -> TableVersionSpec.range(TableVersionSpec.TableVersionKind.BETWEEN, lit(1), null));
        assertThrows(IllegalArgumentException.class, () -> TablePartitionSpec.partition(java.util.List.of()));
        assertThrows(NullPointerException.class, () -> TableSampleSpec.of(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.PERCENT, null, null));
    }

    @Test
    void recursiveVisitorTraversesTableAccessModifiers() {
        var table = sampled(
            tbl("Sales")
                .withVersion(tableVersionBetween(lit(10), lit(20)))
                .withPartitionSpec(tablePartition("SALES_Q1")),
            tableSample(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.PERCENT, lit(5), lit(42)));
        var collector = new Collector();

        table.accept(collector);

        assertTrue(collector.visited.contains("SampledTable"));
        assertTrue(collector.visited.contains("Table"));
        assertTrue(collector.visited.contains("TableVersionSpec"));
        assertTrue(collector.visited.contains("TablePartitionSpec"));
        assertTrue(collector.visited.contains("TableSampleSpec"));
        assertEquals(4, collector.literalCount);
    }

    @Test
    void recursiveTransformerReturnsSameInstanceWhenUnchangedAndNewNodesWhenExpressionsChange() {
        var table = sampled(
            tbl("sales")
                .withVersion(tableVersionBetween(lit(10), lit(20)))
                .withPartitionSpec(tablePartition("sales_q1")),
            tableSample(TableSampleSpec.SampleMethod.SYSTEM, TableSampleSpec.SampleUnit.PERCENT, lit(5), lit(42)));

        assertSame(table, new RecursiveNodeTransformer() {
        }.transform(table));

        var transformed = assertInstanceOf(SampledTable.class, new IncrementLiteralsTransformer().transform(table));
        assertNotSame(table, transformed);
        assertNotSame(table.source(), transformed.source());
        assertNotSame(table.sampleSpec(), transformed.sampleSpec());
        var source = assertInstanceOf(Table.class, transformed.source());
        assertEquals(11, source.version().start().matchExpression().literal(l -> l.value()).orElseThrow(AssertionError::new));
        assertEquals(6, transformed.sampleSpec().amount().matchExpression().literal(l -> l.value()).orElseThrow(AssertionError::new));
    }

    @Test
    void identifierNormalizationUpdatesPartitionNames() {
        var table = tbl("Sales").withPartitionSpec(tablePartition("SALES_Q1", "Sales_Q2"));

        var normalized = assertInstanceOf(Table.class, new IdentifierNormalizationTransformer().transform(table));

        assertEquals("sales", normalized.name().value());
        assertEquals("sales_q1", normalized.partitionSpec().names().getFirst().value());
        assertEquals("sales_q2", normalized.partitionSpec().names().get(1).value());
    }

    private static final class Collector extends RecursiveNodeVisitor<Void> {
        final Set<String> visited = ConcurrentHashMap.newKeySet();
        int literalCount;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitSampledTable(SampledTable t) {
            visited.add("SampledTable");
            return super.visitSampledTable(t);
        }

        @Override
        public Void visitTable(Table t) {
            visited.add("Table");
            return super.visitTable(t);
        }

        @Override
        public Void visitTableVersionSpec(TableVersionSpec spec) {
            visited.add("TableVersionSpec");
            return super.visitTableVersionSpec(spec);
        }

        @Override
        public Void visitTablePartitionSpec(TablePartitionSpec spec) {
            visited.add("TablePartitionSpec");
            return super.visitTablePartitionSpec(spec);
        }

        @Override
        public Void visitTableSampleSpec(TableSampleSpec sample) {
            visited.add("TableSampleSpec");
            return super.visitTableSampleSpec(sample);
        }

        @Override
        public Void visitLiteralExpr(LiteralExpr l) {
            literalCount++;
            return super.visitLiteralExpr(l);
        }
    }

    private static final class IncrementLiteralsTransformer extends RecursiveNodeTransformer {
        @Override
        public Node visitLiteralExpr(LiteralExpr l) {
            if (l.value() instanceof Integer value) {
                return lit(value + 1);
            }
            return l;
        }
    }
}
