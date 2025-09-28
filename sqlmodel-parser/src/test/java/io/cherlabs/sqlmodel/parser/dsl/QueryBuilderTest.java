package io.cherlabs.sqlmodel.parser.dsl;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.core.views.Columns;
import io.cherlabs.sqlmodel.core.views.Tables;
import io.cherlabs.sqlmodel.parser.TableSpecParser;
import io.cherlabs.sqlmodel.parser.repos.DefaultSpecParsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for QueryBuilder using real SpecParsers via a fake SpecParserRepository.
 */
class QueryBuilderTest {

    @Test
    @DisplayName("Builds a simple SELECT ... FROM ... WHERE ... JOIN ... GROUP BY ... ORDER BY with specs")
    void end_to_end_with_specs() {
        QueryBuilder qb = QueryBuilder.newBuilder();

        qb.select("p.id", "lower(name) lname")
                .from("sales.products p")
                .where("status IN ('A','B')")
                .innerJoin("dep d on p.dept_id = d.id")
                .groupBy("p.id")
                .orderBy("lname")
                .limit(10);

        Query q = qb.build();

        assertEquals(2, q.select().size());
        assertTrue(q.select().stream().anyMatch(c -> c instanceof NamedColumn n && "id".equals(n.name()) && "p".equals(n.table())));
        assertTrue(q.select().stream().anyMatch(c -> c instanceof FunctionColumn f && "lower".equals(f.name()) && "lname".equals(f.alias())));

        assertNotNull(q.from());
        assertInstanceOf(NamedTable.class, q.from());
        NamedTable t = (NamedTable) q.from();
        assertEquals("sales", t.schema());
        assertEquals("products", t.name());
        assertEquals("p", t.alias());

        assertInstanceOf(ColumnFilter.class, q.where());

        assertEquals(1, q.joins().size());
        TableJoin j = (TableJoin) q.joins().iterator().next();
        assertEquals(Join.JoinType.Inner, j.joinType());
        assertEquals(Optional.of("dep"), Tables.name(j.table()));
        assertEquals(Optional.of("d"), Tables.alias(j.table()));
        assertNotNull(j.on());

        assertEquals(1, q.groupBy().size());
        assertTrue(q.groupBy().stream().anyMatch(g -> "id".equals(Columns.name(g.column()).orElse("")) && "p".equals(Columns.table(g.column()).orElse(""))));
        assertEquals(1, q.orderBy().size());
        assertEquals(10, q.limit());
    }

    /* -------------------- helpers -------------------- */

    @Test
    @DisplayName("Invalid spec throws IllegalArgumentException")
    void invalid_spec_throws() {
        QueryBuilder qb = QueryBuilder.newBuilder();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> qb.from("sales.")); // bad table spec
        assertTrue(ex.getMessage().contains("Expected identifier"));
    }

    @Test
    @DisplayName("rightJoin(tableSpec, onSpec) builds RIGHT join with ON filter")
    void right_join_helper() {
        QueryBuilder qb = QueryBuilder.newBuilder();

        qb.from("products p")
                .rightJoin("dep d on p.dept_id = d.id");

        Query q = qb.build();
        assertEquals(1, q.joins().size());
        TableJoin j = (TableJoin) q.joins().iterator().next();
        assertEquals(Join.JoinType.Right, j.joinType());
        assertEquals(Optional.of("dep"), Tables.name(j.table()));
        assertEquals(Optional.of("d"), Tables.alias(j.table()));
        assertNotNull(j.on());
    }

    @Test
    @DisplayName("fullJoin(tableSpec, onSpec) builds FULL join with ON filter")
    void full_join_helper() {
        QueryBuilder qb = QueryBuilder.newBuilder();

        qb.from("products p")
                .fullJoin("dep d on p.dept_id = d.id");

        Query q = qb.build();
        assertEquals(1, q.joins().size());
        TableJoin j = (TableJoin) q.joins().iterator().next();
        assertEquals(Join.JoinType.Full, j.joinType());
        assertEquals(Optional.of("dep"), Tables.name(j.table()));
        assertEquals(Optional.of("d"), Tables.alias(j.table()));
        assertNotNull(j.on());
    }

    @Test
    @DisplayName("crossJoin(tableSpec) builds CROSS join without ON")
    void cross_join_helper() {
        QueryBuilder qb = QueryBuilder.newBuilder();

        qb.from("products p")
                .crossJoin("regions r");

        Query q = qb.build();
        assertEquals(1, q.joins().size());
        TableJoin j = (TableJoin) q.joins().iterator().next();
        assertEquals(Join.JoinType.Cross, j.joinType());
        assertEquals(Optional.of("regions"), Tables.name(j.table()));
        assertEquals(Optional.of("r"), Tables.alias(j.table()));
        assertNull(j.on(), "CROSS JOIN must not have ON filter");
    }

    @Test
    @DisplayName("innerJoin(tableSpec, onSpec) still works when multiple joins are chained")
    void inner_join_chained() {
        QueryBuilder qb = QueryBuilder.newBuilder();

        qb.from("products p")
                .innerJoin("inner join dep d on p.dept_id = d.id")
                .leftJoin("left join prices pr on p.id = pr.product_id")
                .rightJoin("right join stock s on p.id = s.product_id");

        Query q = qb.build();
        assertEquals(3, q.joins().size());

        var it = q.joins().iterator();
        TableJoin j1 = (TableJoin) it.next();
        TableJoin j2 = (TableJoin) it.next();
        TableJoin j3 = (TableJoin) it.next();

        assertEquals(Join.JoinType.Inner, j1.joinType());
        assertEquals(Join.JoinType.Left, j2.joinType());
        assertEquals(Join.JoinType.Right, j3.joinType());

        assertNotNull(j1.on());
        assertNotNull(j2.on());
        assertNotNull(j3.on());
    }

    @Test
    void ansi_defaults_work() {
        var qb = QueryBuilder.newBuilder();
        var q = qb.select("p.id")
                .from("sales.products p")
                .build();
        assertInstanceOf(Query.class, q);
        assertEquals(1, q.select().size());
        assertNotNull(q.from());
    }

    @Test
    void custom_repo_and_dialect() {
        var repo = new DefaultSpecParsersRepository();
        repo.register(Table.class, new TableSpecParser());
        var qb = QueryBuilder.newBuilder(repo);
        var q = qb.from("srv.db.schema.tbl t").build();
        assertNotNull(q.from());
        assertEquals("tbl", ((io.cherlabs.sqlmodel.core.NamedTable) q.from()).name());
    }
}
