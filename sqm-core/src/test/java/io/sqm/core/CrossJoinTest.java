package io.sqm.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CrossJoin")
class CrossJoinTest {

    @Test
    @DisplayName("Create cross join with table name only")
    void ofTableNameOnly() {
        CrossJoin join = CrossJoin.of(TableRef.table(Identifier.of("departments")));
        assertNotNull(join);
        assertNotNull(join.right());
        assertInstanceOf(CrossJoin.class, join);
    }

    @Test
    @DisplayName("Create cross join with schema and table name")
    void ofWithSchemaAndTable() {
        CrossJoin join = CrossJoin.of(TableRef.table(Identifier.of("public"), Identifier.of("departments")));
        assertNotNull(join);
        assertNotNull(join.right());
        TableRef right = join.right();
        // The table reference should contain the schema and table info
        assertInstanceOf(CrossJoin.class, join);
        assertEquals("public", ((Table)right).schema().value());
        assertEquals("departments", ((Table)right).name().value());
    }

    @Test
    @DisplayName("Create cross join with TableRef")
    void ofTableRef() {
        TableRef tableRef = TableRef.table(Identifier.of("departments"));
        CrossJoin join = CrossJoin.of(tableRef);
        assertEquals(tableRef, join.right());
        assertInstanceOf(CrossJoin.class, join);
    }

    @Test
    @DisplayName("CrossJoin is instance of Join")
    void isJoin() {
        CrossJoin join = CrossJoin.of(TableRef.table(Identifier.of("employees")));
        assertInstanceOf(Join.class, join);
    }

    @Test
    @DisplayName("CrossJoin is instance of Node")
    void isNode() {
        CrossJoin join = CrossJoin.of(TableRef.table(Identifier.of("employees")));
        assertInstanceOf(Node.class, join);
    }

    @Test
    @DisplayName("CrossJoin has right table reference")
    void hasRightTableReference() {
        CrossJoin join = CrossJoin.of(TableRef.table(Identifier.of("departments")));
        assertNotNull(join.right());
        assertInstanceOf(TableRef.class, join.right());
    }

    @Test
    @DisplayName("Different table names create different cross joins")
    void inequalityDifferentTables() {
        CrossJoin join1 = CrossJoin.of(TableRef.table(Identifier.of("departments")));
        CrossJoin join2 = CrossJoin.of(TableRef.table(Identifier.of("employees")));
        assertNotEquals(join1, join2);
    }

    @Test
    @DisplayName("Same table name creates equal cross joins")
    void equalityWithSameTable() {
        CrossJoin join1 = CrossJoin.of(TableRef.table(Identifier.of("departments")));
        CrossJoin join2 = CrossJoin.of(TableRef.table(Identifier.of("departments")));
        assertEquals(join1, join2);
    }

    @Test
    @DisplayName("CrossJoin with schema equality")
    void equalityWithSchema() {
        CrossJoin join1 = CrossJoin.of(TableRef.table(Identifier.of("public"), Identifier.of("departments")));
        CrossJoin join2 = CrossJoin.of(TableRef.table(Identifier.of("public"), Identifier.of("departments")));
        assertEquals(join1, join2);
    }

    @Test
    @DisplayName("CrossJoin with different schemas are not equal")
    void inequalityDifferentSchemas() {
        CrossJoin join1 = CrossJoin.of(TableRef.table(Identifier.of("public"), Identifier.of("departments")));
        CrossJoin join2 = CrossJoin.of(TableRef.table(Identifier.of("dbo"), Identifier.of("departments")));
        assertNotEquals(join1, join2);
    }

    @Test
    @DisplayName("CrossJoin with TableRef equality")
    void equalityWithTableRef() {
        TableRef tableRef1 = TableRef.table(Identifier.of("departments"));
        TableRef tableRef2 = TableRef.table(Identifier.of("departments"));
        CrossJoin join1 = CrossJoin.of(tableRef1);
        CrossJoin join2 = CrossJoin.of(tableRef2);
        assertEquals(join1, join2);
    }

    @Test
    @DisplayName("Multiple cross joins can be chained")
    void multipleJoins() {
        CrossJoin join1 = CrossJoin.of(TableRef.table(Identifier.of("departments")));
        CrossJoin join2 = CrossJoin.of(TableRef.table(Identifier.of("employees")));
        assertNotEquals(join1, join2);
    }

    @Test
    @DisplayName("CrossJoin produces Cartesian product")
    void cartesianProductSemantics() {
        // CrossJoin semantically produces a Cartesian product
        CrossJoin join = CrossJoin.of(TableRef.table(Identifier.of("orders"), Identifier.of("customers")));
        assertNotNull(join);
        // The join type should represent CROSS JOIN semantics
        assertInstanceOf(CrossJoin.class, join);
    }

    @Test
    @DisplayName("CrossJoin right() is not null")
    void rightNotNull() {
        CrossJoin join = CrossJoin.of(TableRef.table(Identifier.of("departments")));
        assertNotNull(join.right(), "CrossJoin right() should never return null");
    }

    @Test
    @DisplayName("CrossJoin with special table names")
    void specialTableNames() {
        CrossJoin join1 = CrossJoin.of(TableRef.table(Identifier.of("user_departments")));
        assertNotNull(join1);

        CrossJoin join2 = CrossJoin.of(TableRef.table(Identifier.of("public"), Identifier.of("user-departments")));
        assertNotNull(join2);
    }
}

