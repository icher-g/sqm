package io.sqm.core.transform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableQualificationTest {

    @Test
    void unresolved_and_ambiguous_are_singletons() {
        assertEquals(TableQualification.unresolved(), TableQualification.unresolved());
        assertEquals(TableQualification.ambiguous(), TableQualification.ambiguous());
        assertInstanceOf(TableQualification.Unresolved.class, TableQualification.unresolved());
        assertInstanceOf(TableQualification.Ambiguous.class, TableQualification.ambiguous());
    }

    @Test
    void qualified_contains_schema() {
        var qualified = TableQualification.qualified("app");
        assertInstanceOf(TableQualification.Qualified.class, qualified);
        assertEquals("app", ((TableQualification.Qualified) qualified).schema());
    }

    @Test
    void qualified_rejects_null_schema() {
        assertThrows(NullPointerException.class, () -> TableQualification.qualified(null));
    }
}
