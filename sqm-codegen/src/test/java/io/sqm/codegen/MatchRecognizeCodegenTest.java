package io.sqm.codegen;

import io.sqm.core.AfterMatchSkip;
import io.sqm.core.Query;
import io.sqm.core.QuoteStyle;
import io.sqm.core.RowsPerMatch;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchRecognizeCodegenTest {
    private static final String COMPACT_SQL = """
        SELECT *
        FROM sales MATCH_RECOGNIZE (
          PATTERN (A)
          DEFINE A AS A.amount > 0
        ) mr
        """;

    private static final String FULL_PATTERN_SQL = """
        SELECT *
        FROM sales MATCH_RECOGNIZE (
          PARTITION BY customer_id
          ORDER BY sale_date
          ALL ROWS PER MATCH
          AFTER MATCH SKIP TO FIRST B
          PATTERN (^ PERMUTE(A, B) (C | D)* {- E+ -} () F? G{2,4}? $)
          SUBSET U = (A, B)
          DEFINE A AS A.amount > 0,
                 B AS B.amount > 0,
                 C AS C.amount > 0,
                 D AS D.amount > 0,
                 E AS E.amount > 0,
                 F AS F.amount > 0,
                 G AS G.amount > 0
        ) mr
        """;

    private static final String EXPRESSION_HEAVY_SQL = """
        SELECT *
        FROM sales MATCH_RECOGNIZE (
          MEASURES MATCH_NUMBER() AS match_no,
                   CLASSIFIER() AS label,
                   CLASSIFIER(A) AS a_label,
                   FIRST(A.amount) AS first_amount,
                   FINAL LAST(B.amount, 1) AS last_amount,
                   RUNNING PREV(B.amount, 2) AS previous_amount,
                   NEXT(B.amount, 1) AS next_amount
          ONE ROW PER MATCH
          AFTER MATCH SKIP TO NEXT ROW
          PATTERN (A B+?)
          DEFINE A AS A.amount > 0,
                 B AS B.amount > PREV(B.amount)
        ) mr
        """;

    @TempDir
    Path tempDir;

    @Test
    void emitsCompactGoldenDsl() {
        assertEquals("""
            select(
                star()
            )
            .from(matchRecognize(tbl("sales"))
                .rowsPerMatch(oneRowPerMatch())
                .afterMatchSkip(skipPastLastRow())
                .pattern(patternVar(id("A")))
                .define(patternDefinition(id("A"), patternColumn(id("A"), id("amount")).gt(lit(0L))))
                .as(id("mr"))
                .build())
            .build()""", new SqmJavaEmitter().emit(parse(COMPACT_SQL)));
    }

    @Test
    void emitsFullPatternGoldenDsl() {
        assertEquals("""
            select(
                star()
            )
            .from(matchRecognize(tbl("sales"))
                .partitionBy(partition(col("customer_id")))
                .orderBy(orderBy(col("sale_date")))
                .rowsPerMatch(allRowsPerMatch())
                .afterMatchSkip(skipToFirst(id("B")))
                .pattern(patternSequence(patternStart(), patternPermute(patternVar(id("A")), patternVar(id("B"))), zeroOrMore(patternAlternation(patternVar(id("C")), patternVar(id("D")))), excludePattern(oneOrMore(patternVar(id("E")))), emptyPattern(), optionalPattern(patternVar(id("F"))), repeatPattern(patternVar(id("G")), 2, 4, true), patternEnd()))
                .subset(patternSubset(id("U"), id("A"), id("B")))
                .define(patternDefinition(id("A"), patternColumn(id("A"), id("amount")).gt(lit(0L))))
                .define(patternDefinition(id("B"), patternColumn(id("B"), id("amount")).gt(lit(0L))))
                .define(patternDefinition(id("C"), patternColumn(id("C"), id("amount")).gt(lit(0L))))
                .define(patternDefinition(id("D"), patternColumn(id("D"), id("amount")).gt(lit(0L))))
                .define(patternDefinition(id("E"), patternColumn(id("E"), id("amount")).gt(lit(0L))))
                .define(patternDefinition(id("F"), patternColumn(id("F"), id("amount")).gt(lit(0L))))
                .define(patternDefinition(id("G"), patternColumn(id("G"), id("amount")).gt(lit(0L))))
                .as(id("mr"))
                .build())
            .build()""", new SqmJavaEmitter().emit(parse(FULL_PATTERN_SQL)));
    }

    @Test
    void emitsExpressionHeavyGoldenDsl() {
        assertEquals("""
            select(
                star()
            )
            .from(matchRecognize(tbl("sales"))
                .measure(patternMeasure(matchNumber(), id("match_no")))
                .measure(patternMeasure(classifier(), id("label")))
                .measure(patternMeasure(classifier(id("A")), id("a_label")))
                .measure(patternMeasure(first(patternColumn(id("A"), id("amount"))), id("first_amount")))
                .measure(patternMeasure(finalValue(last(patternColumn(id("B"), id("amount")), lit(1L))), id("last_amount")))
                .measure(patternMeasure(running(prev(patternColumn(id("B"), id("amount")), lit(2L))), id("previous_amount")))
                .measure(patternMeasure(next(patternColumn(id("B"), id("amount")), lit(1L)), id("next_amount")))
                .rowsPerMatch(oneRowPerMatch())
                .afterMatchSkip(skipToNextRow())
                .pattern(patternSequence(patternVar(id("A")), repeatPattern(patternVar(id("B")), 1, null, true)))
                .define(patternDefinition(id("A"), patternColumn(id("A"), id("amount")).gt(lit(0L))))
                .define(patternDefinition(id("B"), patternColumn(id("B"), id("amount")).gt(prev(patternColumn(id("B"), id("amount"))))))
                .as(id("mr"))
                .build())
            .build()""", new SqmJavaEmitter().emit(parse(EXPRESSION_HEAVY_SQL)));
    }

    @Test
    void emitsEveryRowsAndVariableSkipOptionWithQuotedIdentifiers() {
        assertEquals(
            "allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.SHOW_EMPTY)",
            emittedOption(
                RowsPerMatch.EmptyMatchHandling.SHOW_EMPTY,
                skipToPattern(id("A B", QuoteStyle.DOUBLE_QUOTE)),
                "skipToPattern(id(\"A B\", QuoteStyle.DOUBLE_QUOTE))"
            )
        );
        assertEquals(
            "allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.OMIT_EMPTY)",
            emittedOption(
                RowsPerMatch.EmptyMatchHandling.OMIT_EMPTY,
                skipToLast(id("A B", QuoteStyle.DOUBLE_QUOTE)),
                "skipToLast(id(\"A B\", QuoteStyle.DOUBLE_QUOTE))"
            )
        );
        assertEquals(
            "allRowsPerMatch(RowsPerMatch.EmptyMatchHandling.WITH_UNMATCHED)",
            emittedOption(
                RowsPerMatch.EmptyMatchHandling.WITH_UNMATCHED,
                skipToFirst(id("A B", QuoteStyle.DOUBLE_QUOTE)),
                "skipToFirst(id(\"A B\", QuoteStyle.DOUBLE_QUOTE))"
            )
        );
    }

    @Test
    void generatedOracleDslCompilesAndReconstructsEquivalentTreesAndSql() throws Exception {
        var sqlDir = tempDir.resolve("sql");
        var outDir = tempDir.resolve("generated");
        var fixtures = new LinkedHashMap<String, String>();
        fixtures.put("compact", COMPACT_SQL);
        fixtures.put("full_pattern", FULL_PATTERN_SQL);
        fixtures.put("expression_heavy", EXPRESSION_HEAVY_SQL);
        for (var fixture : fixtures.entrySet()) {
            var path = sqlDir.resolve("match/" + fixture.getKey() + ".sql");
            Files.createDirectories(path.getParent());
            Files.writeString(path, fixture.getValue(), StandardCharsets.UTF_8);
        }

        var options = SqlFileCodegenOptions.of(
            sqlDir,
            outDir,
            "io.sqm.codegen.generated",
            SqlCodegenDialect.ORACLE
        );
        var generated = SqlFileCodeGenerator.of(options).generate();
        var classes = compile(generated, tempDir.resolve("classes"));

        try (var loader = URLClassLoader.newInstance(
            new URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            for (var fixture : fixtures.entrySet()) {
                var expected = parse(fixture.getValue());
                var actual = invoke(loader, NameNormalizer.toMethodName(fixture.getKey()));
                assertEquals(expected, actual, "Generated tree mismatch for " + fixture.getKey());
                assertEquals(
                    normalizeSql(render(expected)),
                    normalizeSql(render(actual)),
                    "Generated Oracle SQL mismatch for " + fixture.getKey()
                );
            }
        }
    }

    private static Query parse(String sql) {
        var result = ParseContext.of(new OracleSpecs()).parse(Query.class, sql);
        assertTrue(result.ok(), result.errorMessage());
        return result.value();
    }

    private static String emittedOption(
        RowsPerMatch.EmptyMatchHandling handling,
        AfterMatchSkip skip,
        String expectedSkip
    ) {
        var quoted = id("A B", QuoteStyle.DOUBLE_QUOTE);
        var statement = select(star())
            .from(matchRecognize(tbl("sales"))
                .rowsPerMatch(allRowsPerMatch(handling))
                .afterMatchSkip(skip)
                .pattern(patternVar(quoted))
                .define(patternDefinition(quoted, patternColumn(quoted, id("amount")).gt(lit(0L))))
                .as(id("Result Alias", QuoteStyle.DOUBLE_QUOTE))
                .build())
            .build();
        var emitted = new SqmJavaEmitter().emit(statement);

        assertTrue(emitted.contains(".afterMatchSkip(" + expectedSkip + ")"));
        assertTrue(emitted.contains(".pattern(patternVar(id(\"A B\", QuoteStyle.DOUBLE_QUOTE)))"));
        assertTrue(emitted.contains(".as(id(\"Result Alias\", QuoteStyle.DOUBLE_QUOTE))"));
        assertFalse(emitted.contains(".Impl"));
        return emitted.lines()
            .map(String::trim)
            .filter(line -> line.startsWith(".rowsPerMatch("))
            .map(line -> line.substring(".rowsPerMatch(".length(), line.length() - 1))
            .findFirst()
            .orElseThrow();
    }

    private static String render(Query query) {
        return RenderContext.of(new OracleDialect()).render(query).sql();
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static Path compile(List<Path> sources, Path classes) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler is required for generated DSL tests");
        Files.createDirectories(classes);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnostics, null, StandardCharsets.UTF_8)) {
            var units = fileManager.getJavaFileObjectsFromPaths(sources);
            var success = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString()),
                null,
                units
            ).call();
            assertEquals(Boolean.TRUE, success, "Generated DSL failed to compile: " + diagnostics.getDiagnostics());
        }
        return classes;
    }

    private static Query invoke(ClassLoader loader, String methodName) throws Exception {
        try {
            var generatedClass = Class.forName("io.sqm.codegen.generated.MatchQueries", true, loader);
            return assertInstanceOf(Query.class, generatedClass.getMethod(methodName).invoke(null));
        }
        catch (InvocationTargetException ex) {
            throw new IllegalStateException("Generated method failed: " + methodName, ex.getTargetException());
        }
    }
}
