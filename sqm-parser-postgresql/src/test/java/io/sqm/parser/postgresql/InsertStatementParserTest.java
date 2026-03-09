package io.sqm.parser.postgresql;

import io.sqm.core.InsertStatement;
import io.sqm.core.RowExpr;
import io.sqm.core.Statement;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsertStatementParserTest {

    @Test
    void parsesInsertReturningSingleItem() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1) RETURNING id");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertInstanceOf(RowExpr.class, statement.source());
        assertEquals(1, statement.returning().size());
    }

    @Test
    void statementEntryPointParsesInsertReturning() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(Statement.class, "INSERT INTO users (id) VALUES (1) RETURNING id AS user_id");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(InsertStatement.class, result.value());
        assertEquals(1, ((InsertStatement) result.value()).returning().size());
    }
}
