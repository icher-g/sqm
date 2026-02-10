package io.sqm.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlCodegenDialectTest {

    @Test
    void from_resolvesAnsi() {
        assertEquals(SqlCodegenDialect.ANSI, SqlCodegenDialect.from("ansi"));
    }

    @Test
    void from_resolvesPostgresqlAliases() {
        assertEquals(SqlCodegenDialect.POSTGRESQL, SqlCodegenDialect.from("postgresql"));
        assertEquals(SqlCodegenDialect.POSTGRESQL, SqlCodegenDialect.from("postgres"));
        assertEquals(SqlCodegenDialect.POSTGRESQL, SqlCodegenDialect.from("pg"));
    }

    @Test
    void from_throwsOnUnsupportedDialect() {
        assertThrows(IllegalArgumentException.class, () -> SqlCodegenDialect.from("mysql"));
    }
}
