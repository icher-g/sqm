package io.sqm.parser.postgresql;

import io.sqm.core.MergeStatement;
import io.sqm.core.Statement;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeStatementParserTest {

    @Test
    void parsesPostgresMergeFirstSliceWithReturning() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE INTO users AS u
                USING src_users AS s
                ON u.id = s.id
                WHEN MATCHED THEN UPDATE SET name = s.name
                WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name)
                RETURNING id
                """
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().target().name().value());
        assertEquals(2, result.value().clauses().size());
        assertEquals(1, result.value().result().items().size());
    }

    @Test
    void statementEntryPointParsesPostgresMerge() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));
        var result = ctx.parse(
            Statement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(MergeStatement.class, result.value());
    }

    @Test
    void rejectsMergeBeforePostgres15() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(14, 0)));
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("not supported"));
    }

    @Test
    void rejectsReturningWhenCapabilitiesDoNotSupportIt() {
        var ctx = ParseContext.of(new NoReturningPostgresSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE RETURNING id"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("RETURNING"));
    }

    @Test
    void rejectsActionPredicatesAndNotMatchedBySourceInFirstSlice() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));

        var predicates = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED AND src.active = 1 THEN DELETE"
        );
        var bySource = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN NOT MATCHED BY SOURCE THEN DELETE"
        );

        assertTrue(predicates.isError());
        assertTrue(Objects.requireNonNull(predicates.errorMessage()).contains("predicates"));
        assertTrue(bySource.isError());
        assertTrue(Objects.requireNonNull(bySource.errorMessage()).contains("BY"));
    }

    private static final class NoReturningPostgresSpecs implements Specs {
        private final PostgresSpecs delegate = new PostgresSpecs(SqlDialectVersion.of(15, 0));

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
