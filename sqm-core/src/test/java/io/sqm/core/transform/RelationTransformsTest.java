package io.sqm.core.transform;

import io.sqm.core.UpdateStatement;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
}
