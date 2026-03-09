package io.sqm.parser.postgresql;

import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.OperatorPolicy;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.spi.Specs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class UpdateStatementParserTest {

    @Test
    void parsesUpdateFromWithWhere() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(UpdateStatement.class,
            "UPDATE users u SET name = src.name FROM source_users src WHERE u.id = src.id");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals("users", statement.table().name().value());
        assertEquals(1, statement.assignments().size());
        assertEquals(1, statement.from().size());
        assertNotNull(statement.where());
    }

    @Test
    void parsesUpdateWithoutFrom() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertTrue(result.value().from().isEmpty());
        assertNull(result.value().table().alias());
    }

    @Test
    void parsesQuotedKeywordAliasForUpdateTarget() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users AS \"set\" SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().table().alias());
        assertEquals("set", result.value().table().alias().value());
    }

    @Test
    void rejectsKeywordAliasForUpdateTargetAfterAs() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users AS SET name = 'alice'");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected alias after AS"));
    }

    @Test
    void statementEntryPointParsesUpdateFrom() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(Statement.class, "UPDATE users SET name = src.name FROM source_users src WHERE users.id = src.id");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(UpdateStatement.class, result.value());
        assertEquals(1, ((UpdateStatement) result.value()).from().size());
    }

    @Test
    void rejectsUpdateFromWhenCapabilitiesDoNotSupportIt() {
        var ctx = ParseContext.of(new NoUpdateFromPostgresSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = src.name FROM source_users src WHERE users.id = src.id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... FROM is not supported by this dialect"));
    }

    @Test
    void rejectsInvalidFromClause() {
        var ctx = ParseContext.of(new PostgresSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' FROM WHERE id = 1");

        assertTrue(result.isError());
    }

    @Test
    void exposesUpdateStatementTargetType() {
        assertEquals(UpdateStatement.class, new UpdateStatementParser().targetType());
    }

    private static final class NoUpdateFromPostgresSpecs implements Specs {
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
            return feature -> feature != SqlFeature.UPDATE_FROM && delegate.capabilities().supports(feature);
        }

        @Override
        public OperatorPolicy operatorPolicy() {
            return delegate.operatorPolicy();
        }
    }
}
