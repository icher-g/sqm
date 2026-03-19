package io.sqm.parser.postgresql;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.*;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class DeleteStatementParserTest {

    @Test
    void parsesDeleteUsingWithWhere() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users USING source_users src WHERE users.id = src.id");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals("users", statement.table().name().value());
        assertEquals(1, statement.using().size());
        assertNotNull(statement.where());
        assertNull(statement.result());
    }

    @Test
    void parsesDeleteReturning() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users WHERE id = 1 RETURNING id, name");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(2, result.value().result().items().size());
    }

    @Test
    void parsesDeleteWithoutUsing() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users WHERE id = 1");

        assertTrue(result.ok(), result.errorMessage());
        assertTrue(result.value().using().isEmpty());
        assertNull(result.value().result());
        assertNull(result.value().table().alias());
    }

    @Test
    void parsesQuotedKeywordAliasForDeleteTarget() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(DeleteStatement.class,
            "DELETE FROM users AS \"using\" USING source_users src WHERE \"using\".id = src.id");

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().table().alias());
        assertEquals("using", result.value().table().alias().value());
    }

    @Test
    void rejectsKeywordAliasForDeleteTargetAfterAs() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users AS USING WHERE id = 1");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected alias after AS"));
    }

    @Test
    void statementEntryPointParsesDeleteUsing() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(Statement.class, "DELETE FROM users USING source_users s WHERE users.id = s.id RETURNING id");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(DeleteStatement.class, result.value());
        assertEquals(1, ((DeleteStatement) result.value()).using().size());
        assertEquals(1, ((DeleteStatement) result.value()).result().items().size());
    }

    @Test
    void rejectsDeleteUsingWhenCapabilitiesDoNotSupportIt() {
        var ctx = ParseContext.of(new NoDeleteUsingPostgresSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users USING source_users s WHERE users.id = s.id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... USING is not supported by this dialect"));
    }

    @Test
    void rejectsDeleteReturningWhenCapabilitiesDoNotSupportIt() {
        var ctx = ParseContext.of(new NoReturningPostgresSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users WHERE id = 1 RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... RETURNING is not supported by this dialect"));
    }

    @Test
    void rejectsInvalidUsingClause() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users USING WHERE id = 1");

        assertTrue(result.isError());
    }

    @Test
    void exposesDeleteStatementTargetType() {
        assertEquals(DeleteStatement.class, new DeleteStatementParser().targetType());
    }

    private static final class NoDeleteUsingPostgresSpecs implements Specs {
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
            return feature -> feature != SqlFeature.DELETE_USING && delegate.capabilities().supports(feature);
        }

        @Override
        public OperatorPolicy operatorPolicy() {
            return delegate.operatorPolicy();
        }
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
            return feature -> feature != SqlFeature.DML_RESULT_CLAUSE && delegate.capabilities().supports(feature);
        }

        @Override
        public OperatorPolicy operatorPolicy() {
            return delegate.operatorPolicy();
        }
    }
}
