package io.sqm.transpile.builtin;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.transpile.*;
import io.sqm.transpile.rule.DefaultTranspileRuleRegistry;
import io.sqm.transpile.rule.TranspileRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelClauseUnsupportedRuleTest {
    @Test
    void exposesAnAllDialectNonMutatingPreflightGuard() {
        var rule = new ModelClauseUnsupportedRule();
        assertEquals("model-clause-unsupported", rule.id());
        assertTrue(rule.preflight());
        assertEquals(Set.of(), rule.sourceDialects());
        assertEquals(Set.of(), rule.targetDialects());
        assertThrows(NullPointerException.class, () -> new ModelClauseUnsupportedRule(null));
        var result = rule.apply(select(lit(1)).build(), context(SqlDialectId.MYSQL));
        assertTrue(result.problems().isEmpty());
        assertFalse(result.changed());
    }

    @Test
    void retainsTheExactOracleTreeAndCanRejectAnIncapableNativeTarget() {
        var query = modelQuery();
        var result = new ModelClauseUnsupportedRule().apply(query, context(SqlDialectId.ORACLE));
        assertSame(query, result.statement());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        assertTrue(result.problems().isEmpty());
        assertEquals(RewriteFidelity.UNSUPPORTED,
            new ModelClauseUnsupportedRule(feature -> false).apply(query, context(SqlDialectId.ORACLE)).fidelity());
        var incapable = SqlTranspiler.builder().sourceDialect(SqlDialectId.ORACLE).targetDialect(SqlDialectId.ORACLE)
            .renderer(AnsiDialect::new).build().transpile(query);
        assertEquals(TranspileStatus.UNSUPPORTED, incapable.status());
        assertEquals("UNSUPPORTED_MODEL_CLAUSE", incapable.problems().getFirst().code());
    }

    @Test
    void standaloneDefaultRegistryPreservesOnlyItsNativeOraclePair() {
        var registry = DefaultTranspileRuleRegistry.defaults();
        var query = modelQuery();
        for (var target : List.of(SqlDialectId.ORACLE, SqlDialectId.MYSQL)) {
            var guard = registry.rulesFor(SqlDialectId.ORACLE, target).stream()
                .filter(rule -> rule.id().equals("model-clause-unsupported")).findFirst().orElseThrow();
            var result = guard.apply(query, context(target));
            assertEquals(target.equals(SqlDialectId.ORACLE), result.problems().isEmpty());
            assertSame(query, result.statement());
        }
    }

    @Test
    void configuredRegistryDelegatesCapabilitiesAndNativePairPolicyToTheModelRule() {
        var query = modelQuery();
        assertThrows(NullPointerException.class, () -> DefaultTranspileRuleRegistry.defaults(null));
        for (var supported : List.of(true, false)) {
            var registry = DefaultTranspileRuleRegistry.defaults(feature -> supported && feature == SqlFeature.MODEL_CLAUSE);
            for (var target : List.of(SqlDialectId.ORACLE, SqlDialectId.MYSQL)) {
                var guard = registry.rulesFor(SqlDialectId.ORACLE, target).stream()
                    .filter(rule -> rule.id().equals("model-clause-unsupported")).findFirst().orElseThrow();
                var result = guard.apply(query, context(target));
                assertEquals(supported && target.equals(SqlDialectId.ORACLE), result.problems().isEmpty());
                assertSame(query, result.statement());
            }
        }
        var nonNativeSource = new TranspileContext(SqlDialectId.ANSI, SqlDialectId.ORACLE,
            TranspileOptions.defaults(), Optional.empty(), Optional.empty());
        assertFalse(new ModelClauseUnsupportedRule(feature -> true).apply(query, nonNativeSource).problems().isEmpty());
    }

    @Test
    void discoversNestedSourcesReferencesAndGeneratedQueriesWithStablePaths() {
        assertPath(modelQuery(), "select.model[0]");
        assertPath(select(star()).from(tbl(modelQuery()).as("nested")).build(), "select.queryTable.select.model[0]");
        assertPath(with(cte("forecast", modelQuery())).body(select(star()).from(tbl("forecast")).build()),
            "with.cte.select.model[0]");
        assertPath(insert("forecasts").columns(id("sales")).query(modelQuery()).build(), "insert.select.model[0]");
        var reference = referenceModel("r", modelQuery()).dimension("y").measure("sales").build();
        var outer = select(lit(1)).model(model().reference(reference).dimension("y").measure("sales")
            .rule(cellTarget("sales", cellForValues("y", modelQuery())), lit(1)).build()).build();
        var result = new ModelClauseUnsupportedRule().apply(outer, context(SqlDialectId.POSTGRESQL));
        assertEquals(List.of("select.model[0]", "select.model.reference.select.model[0]", "select.model.for.subquery.select.model[0]"),
            result.problems().stream().map(TranspileProblem::clausePath).toList());
    }

    @Test
    void rejectsBeforeOtherRulesCanPartiallyRewriteAndPreservesSequenceMetadata() {
        var query = SelectQuery.builder(modelQuery()).limit(2).build();
        var transpiler = SqlTranspiler.builder().sourceDialect(SqlDialectId.ORACLE).targetDialect(SqlDialectId.SQLSERVER).build();
        var result = transpiler.transpile(query);
        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertSame(query, result.transpiledAst().orElseThrow());
        assertEquals(1, result.steps().size());
        assertTrue(result.sql().isEmpty());
        var sequence = transpiler.transpile(StatementSequence.of(List.of(query, query)));
        assertEquals(List.of(1, 2), sequence.problems().stream().map(TranspileProblem::statementIndex).toList());
        for (var problem : sequence.problems()) {
            assertEquals(SqlDialectId.ORACLE, problem.sourceDialect());
            assertEquals(SqlDialectId.SQLSERVER, problem.targetDialect());
            assertEquals("UNSUPPORTED_MODEL_CLAUSE", problem.code());
        }
    }

    @Test
    void refusesTreeMutationsFromPreflightRules() {
        var guard = new TranspileRule() {
            @Override public boolean preflight() { return true; }
            @Override public String id() { return "unsafe-guard"; }
            @Override public Set<SqlDialectId> sourceDialects() { return Set.of(); }
            @Override public Set<SqlDialectId> targetDialects() { return Set.of(); }
            @Override public TranspileRuleResult apply(Statement statement, TranspileContext context) {
                return new TranspileRuleResult(select(lit(2)).build(), true, RewriteFidelity.EXACT, List.of(), List.of(), "invalid");
            }
        };
        var transpiler = SqlTranspiler.builder().sourceDialect(SqlDialectId.ORACLE).targetDialect(SqlDialectId.ORACLE)
            .registry(DefaultTranspileRuleRegistry.of(List.of(guard))).build();
        assertThrows(IllegalStateException.class, () -> transpiler.transpile(modelQuery()));
    }

    private static void assertPath(Statement query, String path) {
        var result = new ModelClauseUnsupportedRule().apply(query, context(SqlDialectId.POSTGRESQL));
        var problem = result.problems().getFirst();
        assertEquals(path, problem.clausePath());
        assertEquals(SqlDialectId.ORACLE, problem.sourceDialect());
        assertEquals(SqlDialectId.POSTGRESQL, problem.targetDialect());
    }

    private static SelectQuery modelQuery() {
        return select(col("sales")).from(tbl("data")).model(model().dimension("y").measure("sales")
            .rule(cellTarget("sales", 2026), lit(1)).build()).build();
    }

    private static TranspileContext context(SqlDialectId target) {
        return new TranspileContext(SqlDialectId.ORACLE, target, TranspileOptions.defaults(), Optional.empty(), Optional.empty());
    }
}
