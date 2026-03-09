package io.sqm.parser.postgresql;

import io.sqm.core.InsertStatement;
import io.sqm.core.RowExpr;
import io.sqm.core.Statement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.OperatorPolicy;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.spi.Specs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

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
    void parsesInsertWithoutReturning() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1)");

        assertTrue(result.ok(), result.errorMessage());
        assertTrue(result.value().returning().isEmpty());
    }

    @Test
    void statementEntryPointParsesInsertReturning() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(Statement.class, "INSERT INTO users (id) VALUES (1) RETURNING id AS user_id");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(InsertStatement.class, result.value());
        assertEquals(1, ((InsertStatement) result.value()).returning().size());
    }

    @Test
    void rejectsReturningWhenCapabilitiesDoNotSupportIt() {
        var ctx = ParseContext.of(new NoReturningPostgresSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1) RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("INSERT ... RETURNING is not supported by this dialect"));
    }

    @Test
    void rejectsInvalidReturningItems() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users VALUES (1) RETURNING )");

        assertTrue(result.isError());
    }

    private static final class NoReturningPostgresSpecs implements Specs {
        private final PostgresSpecs delegate = new PostgresSpecs();

        @Override
        public ParsersRepository parsers() {
            return delegate.parsers();
        }

        @Override
        public Lookups lookups() {
            return delegate.lookups();
        }

        @Override
        public IdentifierQuoting identifierQuoting() {
            return delegate.identifierQuoting();
        }

        @Override
        public DialectCapabilities capabilities() {
            return feature -> false;
        }

        @Override
        public OperatorPolicy operatorPolicy() {
            return delegate.operatorPolicy();
        }
    }
}
