package io.sqm.parser.postgresql;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeStatement;
import io.sqm.core.MergeUpdateAction;
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

import static org.junit.jupiter.api.Assertions.*;

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
                WHEN MATCHED AND s.active = true THEN UPDATE SET name = s.name
                WHEN NOT MATCHED AND s.name IS NOT NULL THEN INSERT (id, name) VALUES (s.id, s.name)
                RETURNING id
                """
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().target().name().value());
        assertEquals(2, result.value().clauses().size());
        assertTrue(result.value().clauses().stream().allMatch(clause -> clause.condition() != null));
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
    void parsesActionPredicatesAndNotMatchedBySourceClauses() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));

        var predicates = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED AND src.active = 1 THEN DELETE"
        );
        var bySource = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN NOT MATCHED BY SOURCE AND users.active = true THEN UPDATE SET name = src.name"
        );

        assertTrue(predicates.ok(), predicates.errorMessage());
        assertNotNull(predicates.value().clauses().getFirst().condition());
        assertTrue(bySource.ok(), bySource.errorMessage());
        assertEquals(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, bySource.value().clauses().getFirst().matchType());
        assertNotNull(bySource.value().clauses().getFirst().condition());
    }

    @Test
    void parsesDoNothingAcrossSupportedMergeBranches() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN MATCHED THEN DO NOTHING
                WHEN NOT MATCHED AND src.id > 0 THEN DO NOTHING
                WHEN NOT MATCHED BY SOURCE THEN DO NOTHING
                """
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(3, result.value().clauses().size());
        assertTrue(result.value().clauses().stream().allMatch(clause -> clause.action() instanceof MergeDoNothingAction));
    }

    @Test
    void rejectsMergeWithoutClausesAndWhenAfterReturning() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));

        var noClauses = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id"
        );
        var whenAfterReturning = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE RETURNING id WHEN MATCHED THEN DELETE"
        );

        assertTrue(noClauses.isError());
        assertTrue(Objects.requireNonNull(noClauses.errorMessage()).contains("at least one MERGE clause"));
        assertTrue(whenAfterReturning.isError());
        assertTrue(Objects.requireNonNull(whenAfterReturning.errorMessage()).contains("Duplicate or unsupported"));
    }

    @Test
    void parsesClauseAndActionsForCoveredFirstSliceShapes() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));

        var matchedUpdate = ctx.parse(
            MergeClause.class,
            "WHEN MATCHED THEN UPDATE SET name = src.name"
        );
        var notMatchedInsert = ctx.parse(
            MergeClause.class,
            "WHEN NOT MATCHED THEN INSERT VALUES (src.id)"
        );
        var updateAction = ctx.parse(
            MergeUpdateAction.class,
            "UPDATE SET name = src.name, email = src.email"
        );
        var doNothingAction = ctx.parse(
            MergeDoNothingAction.class,
            "DO NOTHING"
        );
        var insertAction = ctx.parse(
            MergeInsertAction.class,
            "INSERT VALUES (src.id, src.name)"
        );

        assertTrue(matchedUpdate.ok(), matchedUpdate.errorMessage());
        assertEquals(MergeClause.MatchType.MATCHED, matchedUpdate.value().matchType());
        assertTrue(notMatchedInsert.ok(), notMatchedInsert.errorMessage());
        assertEquals(MergeClause.MatchType.NOT_MATCHED, notMatchedInsert.value().matchType());
        assertTrue(updateAction.ok(), updateAction.errorMessage());
        assertEquals(2, updateAction.value().assignments().size());
        assertTrue(doNothingAction.ok(), doNothingAction.errorMessage());
        assertTrue(insertAction.ok(), insertAction.errorMessage());
        assertTrue(insertAction.value().columns().isEmpty());
        assertEquals(2, insertAction.value().values().items().size());
    }

    @Test
    void rejectsInvalidClauseShapesAndMalformedActions() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));

        var missingThen = ctx.parse(
            MergeClause.class,
            "WHEN MATCHED DELETE"
        );
        var matchedInsert = ctx.parse(
            MergeClause.class,
            "WHEN MATCHED THEN INSERT VALUES (src.id)"
        );
        var bySourceInsert = ctx.parse(
            MergeClause.class,
            "WHEN NOT MATCHED BY SOURCE THEN INSERT VALUES (src.id)"
        );
        var notMatchedDelete = ctx.parse(
            MergeClause.class,
            "WHEN NOT MATCHED THEN DELETE"
        );
        var invalidDo = ctx.parse(
            MergeDoNothingAction.class,
            "DO UPDATE"
        );
        var invalidUpdate = ctx.parse(
            MergeUpdateAction.class,
            "UPDATE SET"
        );
        var invalidInsert = ctx.parse(
            MergeInsertAction.class,
            "INSERT VALUES src.id"
        );

        assertTrue(missingThen.isError());
        assertTrue(Objects.requireNonNull(missingThen.errorMessage()).contains("Expected THEN"));
        assertTrue(matchedInsert.isError());
        assertTrue(Objects.requireNonNull(matchedInsert.errorMessage()).contains("cannot use INSERT"));
        assertTrue(bySourceInsert.isError());
        assertTrue(Objects.requireNonNull(bySourceInsert.errorMessage()).contains("cannot use INSERT"));
        assertTrue(notMatchedDelete.isError());
        assertTrue(Objects.requireNonNull(notMatchedDelete.errorMessage()).contains("must use INSERT or DO NOTHING"));
        assertTrue(invalidDo.isError());
        assertTrue(Objects.requireNonNull(invalidDo.errorMessage()).contains("NOTHING"));
        assertTrue(invalidUpdate.isError());
        assertTrue(Objects.requireNonNull(invalidUpdate.errorMessage()).contains("assignment"));
        assertTrue(invalidInsert.isError());
        assertTrue(Objects.requireNonNull(invalidInsert.errorMessage()).contains("Expected"));
    }

    @Test
    void rejectsMalformedClausePredicate() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));
        var result = ctx.parse(
            MergeClause.class,
            "WHEN MATCHED AND THEN DELETE"
        );

        assertTrue(result.isError());
    }

    @Test
    void rejectsMergeTop() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE TOP (10) users USING src ON users.id = src.id WHEN MATCHED THEN DELETE"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("TOP"));
    }

    @Test
    void parsesNotMatchedInsertWithPredicateAndNoColumnList() {
        var ctx = ParseContext.of(new PostgresSpecs(SqlDialectVersion.of(15, 0)));
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN NOT MATCHED AND src.id > 0 THEN INSERT VALUES (src.id)"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().clauses().getFirst().condition());
        assertTrue(((MergeInsertAction) result.value().clauses().getFirst().action()).columns().isEmpty());
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
