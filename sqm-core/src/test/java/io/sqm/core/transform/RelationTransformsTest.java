package io.sqm.core.transform;

import io.sqm.core.FunctionTable;
import io.sqm.core.Lateral;
import io.sqm.core.Query;
import io.sqm.core.QueryTable;
import io.sqm.core.Table;
import io.sqm.core.UpdateStatement;
import io.sqm.core.ValuesTable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class RelationTransformsTest {

    @Test
    void renameTableRewritesRelationsAcrossStatements() {
        var statement = update(tbl("users"))
            .join(inner(tbl("audit")).on(col("users", "id").eq(col("audit", "user_id"))))
            .set(set("name", lit("alice")))
            .from(tbl("audit"))
            .result(resultInto(tbl("audit"), "user_id"), col("id"))
            .build();

        UpdateStatement transformed = RelationTransforms.renameTable(statement, "audit", "audit_log");

        assertNotSame(statement, transformed);
        assertEquals("audit_log", transformed.joins().getFirst().right().matchTableRef().table(t -> t.name().value()).orElse(null));
        assertEquals("audit_log", transformed.from().getFirst().matchTableRef().table(t -> t.name().value()).orElse(null));
        assertEquals("audit_log", transformed.result().into().target().matchTableRef().table(t -> t.name().value()).orElse(null));
    }

    @Test
    void rewriteTableRefsSupportsVariableTargets() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .result(resultInto(tableVar("@audit_rows"), "user_id"), col("id"))
            .build();

        UpdateStatement transformed = RelationTransforms.rewriteTableRefs(statement, tableRef -> tableRef.<io.sqm.core.TableRef>matchTableRef()
            .variableTable(variable -> tableVar("@audit_archive"))
            .otherwise(ref -> ref)
        );

        assertNotSame(statement, transformed);
        assertEquals(
            "audit_archive",
            transformed.result().into().target().matchTableRef().variableTable(v -> v.name().value()).orElse(null)
        );
    }

    @Test
    void rewriteTableRefsPreservesIdentityWhenNothingChanges() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        var transformed = RelationTransforms.renameTable(statement, "audit", "audit_log");

        assertSame(statement, transformed);
    }

    @Test
    void rewriteTablesSupportsSchemaUpdates() {
        var query = io.sqm.dsl.Dsl.select(col("u", "id"))
            .from(tbl("users").as("u"))
            .join(inner(tbl("audit")).on(col("u", "id").eq(col("audit", "user_id"))))
            .build();

        var transformed = (io.sqm.core.SelectQuery) RelationTransforms.qualifyUnqualifiedTables(query, id("app"));

        assertNotSame(query, transformed);
        assertEquals("app", ((io.sqm.core.Table) transformed.from()).schema().value());
        assertEquals("app", transformed.joins().getFirst().right().matchTableRef().table(t -> t.schema().value()).orElse(null));
    }

    @Test
    void qualifyUnqualifiedTablesPreservesIdentityWhenNothingNeedsQualification() {
        var query = io.sqm.dsl.Dsl.select(col("id")).from(tbl("app", "users")).build();

        var transformed = RelationTransforms.qualifyUnqualifiedTables(query, id("app"));

        assertSame(query, transformed);
    }

    @Test
    void qualifyUnqualifiedTablesSupportsStringSchemaOverload() {
        Query query = select(col("id")).from(tbl("users")).build();

        var transformed = (io.sqm.core.SelectQuery) RelationTransforms.qualifyUnqualifiedTables(query, "tenant42");

        assertEquals("tenant42", ((Table) transformed.from()).schema().value());
    }

    @Test
    void remapTablesSupportsBulkRuntimeTableNameMapping() {
        var statement = update(tbl("users"))
            .join(inner(tbl("orders")).on(col("users", "id").eq(col("orders", "user_id"))))
            .set(set("name", lit("alice")))
            .from(tbl("orders"))
            .result(resultInto(tbl("audit"), "user_id"), col("id"))
            .build();

        UpdateStatement transformed = RelationTransforms.remapTables(
            statement,
            Map.of("users", "tenant_users", "orders", "tenant_orders", "audit", "tenant_audit")
        );

        assertNotSame(statement, transformed);
        assertEquals("tenant_users", transformed.table().name().value());
        assertEquals("tenant_orders", transformed.joins().getFirst().right().matchTableRef().table(t -> t.name().value()).orElse(null));
        assertEquals("tenant_orders", transformed.from().getFirst().matchTableRef().table(t -> t.name().value()).orElse(null));
        assertEquals("tenant_audit", transformed.result().into().target().matchTableRef().table(t -> t.name().value()).orElse(null));
    }

    @Test
    void remapTablesPreservesIdentityWhenResolverDoesNotChangeNames() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        var transformed = RelationTransforms.remapTables(statement, name -> name);

        assertSame(statement, transformed);
    }

    @Test
    void remapTablesPreservesIdentityWhenResolverReturnsNull() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        var transformed = RelationTransforms.remapTables(statement, name -> null);

        assertSame(statement, transformed);
    }

    @Test
    void rewriteTableRefsVisitsDerivedTableRefVariants() {
        var innerQuery = select(col("id")).from(tbl("orders")).build();
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .from(tbl(rows(row(1))), tbl(func("OPENJSON", lit("{}"))))
            .join(cross(tbl(innerQuery).lateral()))
            .build();

        AtomicInteger tables = new AtomicInteger();
        AtomicInteger queryTables = new AtomicInteger();
        AtomicInteger valuesTables = new AtomicInteger();
        AtomicInteger functionTables = new AtomicInteger();
        AtomicInteger lateralRefs = new AtomicInteger();

        RelationTransforms.rewriteTableRefs(statement, ref -> {
            switch (ref) {
                case Table ignored -> tables.incrementAndGet();
                case QueryTable ignored -> queryTables.incrementAndGet();
                case ValuesTable ignored -> valuesTables.incrementAndGet();
                case FunctionTable ignored -> functionTables.incrementAndGet();
                case Lateral ignored -> lateralRefs.incrementAndGet();
                default -> {
                }
            }
            return ref;
        });

        assertTrue(tables.get() > 0);
        assertEquals(1, queryTables.get());
        assertEquals(1, valuesTables.get());
        assertEquals(1, functionTables.get());
        assertEquals(1, lateralRefs.get());
    }

    @Test
    void rewriteTableRefsRejectsNullResults() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        var ex = assertThrows(
            IllegalArgumentException.class,
            () -> RelationTransforms.rewriteTableRefs(statement, ref -> null)
        );

        assertEquals("TableRef rewriter must not return null for: " + tbl("users"), ex.getMessage());
    }

    @Test
    void rewriteTablesRejectsNullResults() {
        var statement = update(tbl("users"))
            .set(set("name", lit("alice")))
            .build();

        var ex = assertThrows(
            IllegalArgumentException.class,
            () -> RelationTransforms.rewriteTables(statement, table -> null)
        );

        assertEquals("Table rewriter must not return null for: " + tbl("users"), ex.getMessage());
    }
}
