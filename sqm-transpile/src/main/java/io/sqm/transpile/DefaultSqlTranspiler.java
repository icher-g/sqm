package io.sqm.transpile;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.Specs;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.RenderOptions;
import io.sqm.render.spi.SqlDialect;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import io.sqm.transpile.rule.DefaultTranspileRuleRegistry;
import io.sqm.transpile.rule.TranspileRule;
import io.sqm.transpile.rule.TranspileRuleRegistry;
import io.sqm.validate.mysql.MySqlValidationDialect;
import io.sqm.validate.postgresql.PostgresValidationDialect;
import io.sqm.validate.schema.SchemaStatementValidator;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;
import io.sqm.validate.sqlserver.SqlServerValidationDialect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default {@link SqlTranspiler} implementation.
 */
public final class DefaultSqlTranspiler implements SqlTranspiler {
    private final SqlDialectId sourceDialect;
    private final SqlDialectId targetDialect;
    private final Supplier<Specs> parserFactory;
    private final Supplier<SqlDialect> rendererFactory;
    private final Supplier<SchemaValidationSettings> validationFactory;
    private final CatalogSchema sourceSchema;
    private final CatalogSchema targetSchema;
    private final TranspileOptions options;
    private final TranspileRuleRegistry registry;

    private DefaultSqlTranspiler(Builder builder) {
        this.sourceDialect = Objects.requireNonNull(builder.sourceDialect, "sourceDialect");
        this.targetDialect = Objects.requireNonNull(builder.targetDialect, "targetDialect");
        this.parserFactory = builder.parserFactory != null ? builder.parserFactory : defaultParserFactory(sourceDialect);
        this.rendererFactory = builder.rendererFactory != null ? builder.rendererFactory : defaultRendererFactory(targetDialect);
        this.validationFactory = builder.validationFactory != null ? builder.validationFactory : defaultValidationFactory(targetDialect);
        this.sourceSchema = builder.sourceSchema;
        this.targetSchema = builder.targetSchema;
        this.options = builder.options != null ? builder.options : TranspileOptions.defaults();
        this.registry = builder.registry != null ? builder.registry : DefaultTranspileRuleRegistry.defaults();
    }

    private static TranspileStatus highestFailureStatus(List<TranspileProblem> problems) {
        if (problems.stream().anyMatch(problem -> problem.stage() == TranspileStage.VALIDATE)) {
            return TranspileStatus.VALIDATION_FAILED;
        }
        if (problems.stream().anyMatch(problem -> problem.stage() == TranspileStage.RENDER)) {
            return TranspileStatus.RENDER_FAILED;
        }
        return TranspileStatus.UNSUPPORTED;
    }

    private static List<TranspileProblem> parseProblems(io.sqm.parser.spi.ParseResult<?> parseResult) {
        var problems = new ArrayList<TranspileProblem>(parseResult.problems().size());
        for (var problem : parseResult.problems()) {
            problems.add(new TranspileProblem(
                "PARSE_ERROR",
                problem.message(),
                TranspileStage.PARSE,
                problem.pos(),
                problem.line(),
                problem.column()
            ));
        }
        return List.copyOf(problems);
    }

    private static List<TranspileProblem> validationProblems(List<io.sqm.validate.api.ValidationProblem> validationProblems) {
        var problems = new ArrayList<TranspileProblem>(validationProblems.size());
        for (var problem : validationProblems) {
            problems.add(new TranspileProblem(problem.code().name(), problem.message(), TranspileStage.VALIDATE));
        }
        return List.copyOf(problems);
    }

    private static Supplier<Specs> defaultParserFactory(SqlDialectId dialectId) {
        if (SqlDialectId.ANSI.equals(dialectId)) {
            return AnsiSpecs::new;
        }
        if (SqlDialectId.MYSQL.equals(dialectId)) {
            return MySqlSpecs::new;
        }
        if (SqlDialectId.POSTGRESQL.equals(dialectId)) {
            return PostgresSpecs::new;
        }
        if (SqlDialectId.SQLSERVER.equals(dialectId)) {
            return SqlServerSpecs::new;
        }
        throw new IllegalArgumentException("Unsupported source dialect: " + dialectId.value());
    }

    private static Supplier<SqlDialect> defaultRendererFactory(SqlDialectId dialectId) {
        if (SqlDialectId.ANSI.equals(dialectId)) {
            return AnsiDialect::new;
        }
        if (SqlDialectId.MYSQL.equals(dialectId)) {
            return MySqlDialect::new;
        }
        if (SqlDialectId.POSTGRESQL.equals(dialectId)) {
            return PostgresDialect::new;
        }
        if (SqlDialectId.SQLSERVER.equals(dialectId)) {
            return SqlServerDialect::new;
        }
        throw new IllegalArgumentException("Unsupported target dialect: " + dialectId.value());
    }

    private static Supplier<SchemaValidationSettings> defaultValidationFactory(SqlDialectId dialectId) {
        return () -> mergeDialectSettings(
            SqlDialectId.MYSQL.equals(dialectId)
                ? MySqlValidationDialect.of()
                : SqlDialectId.POSTGRESQL.equals(dialectId)
                  ? PostgresValidationDialect.of()
                  : SqlDialectId.ANSI.equals(dialectId)
                    ? null
                    : SqlDialectId.SQLSERVER.equals(dialectId)
                      ? SqlServerValidationDialect.of()
                      : unsupportedValidationDialect(dialectId)
        );
    }

    private static SchemaValidationDialect unsupportedValidationDialect(SqlDialectId dialectId) {
        throw new IllegalArgumentException("Unsupported validation dialect: " + dialectId.value());
    }

    private static SchemaValidationSettings mergeDialectSettings(SchemaValidationDialect dialect) {
        if (dialect == null) {
            return SchemaValidationSettings.defaults();
        }
        var base = SchemaValidationSettings.defaults();
        return SchemaValidationSettings.builder()
            .functionCatalog(dialect.functionCatalog())
            .accessPolicy(base.accessPolicy())
            .principal(base.principal())
            .tenant(base.tenant())
            .tenantRequirementMode(base.tenantRequirementMode())
            .limits(base.limits())
            .addRules(dialect.additionalRules())
            .addRules(base.additionalRules())
            .build();
    }

    @Override
    public TranspileResult transpile(String sql) {
        Objects.requireNonNull(sql, "sql");
        var ctx = ParseContext.of(parserFactory.get());
        var parseResult = ctx.parse(StatementSequence.class, sql);
        if (parseResult.isError() || parseResult.value() == null) {
            return new TranspileResult(
                TranspileStatus.PARSE_FAILED,
                null,
                null,
                null,
                List.of(),
                parseProblems(parseResult),
                List.of()
            );
        }
        var sequence = parseResult.value();
        return transpile(sequence);
    }

    @Override
    public TranspileResult transpile(Statement statement) {
        Objects.requireNonNull(statement, "statement");
        var context = new TranspileContext(
            sourceDialect,
            targetDialect,
            options,
            Optional.ofNullable(sourceSchema),
            Optional.ofNullable(targetSchema)
        );

        Statement current = statement;
        var steps = new ArrayList<TranspileStep>();
        var warnings = new ArrayList<TranspileWarning>();
        var problems = new ArrayList<TranspileProblem>();

        for (TranspileRule rule : registry.rulesFor(sourceDialect, targetDialect)) {
            var result = rule.apply(current, context);
            current = result.statement();

            steps.add(new TranspileStep(rule.id(), result.fidelity(), result.description(), result.changed()));
            warnings.addAll(result.warnings());
            problems.addAll(result.problems());

            if (result.fidelity() == RewriteFidelity.APPROXIMATE && !options.allowApproximateRewrites()) {
                problems.add(new TranspileProblem(
                    "APPROXIMATE_REWRITE_DISABLED",
                    "Approximate rewrite is disabled by transpilation options",
                    TranspileStage.REWRITE
                ));
            }
        }

        if (!problems.isEmpty()) {
            return new TranspileResult(
                TranspileStatus.UNSUPPORTED,
                statement,
                current,
                null,
                steps,
                problems,
                warnings
            );
        }

        if (options.failOnWarnings() && !warnings.isEmpty()) {
            return new TranspileResult(
                TranspileStatus.UNSUPPORTED,
                statement,
                current,
                null,
                steps,
                List.of(new TranspileProblem(
                    "WARNINGS_NOT_ALLOWED",
                    "Warnings are not allowed by transpilation options",
                    TranspileStage.REWRITE
                )),
                warnings
            );
        }

        if (options.validateTarget() && targetSchema != null) {
            var validationResult = SchemaStatementValidator.of(targetSchema, validationFactory.get()).validate(current);
            if (!validationResult.ok()) {
                return new TranspileResult(
                    TranspileStatus.VALIDATION_FAILED,
                    statement,
                    current,
                    null,
                    steps,
                    validationProblems(validationResult.problems()),
                    warnings
                );
            }
        }

        Optional<String> sql = Optional.empty();
        List<Object> params = List.of();
        if (options.renderSql()) {
            try {
                var text = RenderContext.of(rendererFactory.get()).render(current, RenderOptions.of(options.renderParameterizationMode()));
                sql = Optional.of(text.sql());
                params = text.params();
            } catch (RuntimeException ex) {
                return new TranspileResult(
                    TranspileStatus.RENDER_FAILED,
                    statement,
                    current,
                    null,
                    steps,
                    List.of(new TranspileProblem("RENDER_FAILED", ex.getMessage(), TranspileStage.RENDER)),
                    warnings
                );
            }
        }

        return new TranspileResult(
            warnings.isEmpty() ? TranspileStatus.SUCCESS : TranspileStatus.SUCCESS_WITH_WARNINGS,
            Optional.of(statement),
            Optional.of(current),
            sql,
            params,
            steps,
            List.of(),
            warnings
        );
    }

    @Override
    public TranspileResult transpile(StatementSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");

        var transpiledStatements = new ArrayList<Statement>(sequence.statements().size());
        var steps = new ArrayList<TranspileStep>();
        var warnings = new ArrayList<TranspileWarning>();
        var problems = new ArrayList<TranspileProblem>();

        int statementIndex = 1;
        for (Statement statement : sequence.statements()) {
            int currentStatementIndex = statementIndex;
            var result = transpile(statement);
            steps.addAll(result.steps());
            result.warnings().stream()
                .map(warning -> warning.withStatementIndex(currentStatementIndex))
                .forEach(warnings::add);
            result.problems().stream()
                .map(problem -> problem.withStatementIndex(currentStatementIndex))
                .forEach(problems::add);

            if (result.transpiledAst().orElse(null) instanceof Statement transpiledStatement) {
                transpiledStatements.add(transpiledStatement);
            }
            statementIndex++;
        }

        if (!problems.isEmpty()) {
            return new TranspileResult(
                highestFailureStatus(problems),
                sequence,
                null,
                null,
                steps,
                problems,
                warnings
            );
        }

        var transpiledSequence = StatementSequence.of(transpiledStatements);
        Optional<String> sql = Optional.empty();
        List<Object> params = List.of();
        if (options.renderSql()) {
            try {
                var text = RenderContext.of(rendererFactory.get()).render(transpiledSequence, RenderOptions.of(options.renderParameterizationMode()));
                sql = Optional.of(text.sql());
                params = text.params();
            } catch (RuntimeException ex) {
                return new TranspileResult(
                    TranspileStatus.RENDER_FAILED,
                    sequence,
                    transpiledSequence,
                    null,
                    steps,
                    List.of(new TranspileProblem("RENDER_FAILED", ex.getMessage(), TranspileStage.RENDER)),
                    warnings
                );
            }
        }

        return new TranspileResult(
            warnings.isEmpty() ? TranspileStatus.SUCCESS : TranspileStatus.SUCCESS_WITH_WARNINGS,
            Optional.of(sequence),
            Optional.of(transpiledSequence),
            sql,
            params,
            steps,
            List.of(),
            warnings
        );
    }

    /**
     * Builder for {@link DefaultSqlTranspiler}.
     */
    public static final class Builder implements SqlTranspiler.Builder {
        private SqlDialectId sourceDialect;
        private SqlDialectId targetDialect;
        private Supplier<Specs> parserFactory;
        private Supplier<SqlDialect> rendererFactory;
        private Supplier<SchemaValidationSettings> validationFactory;
        private CatalogSchema sourceSchema;
        private CatalogSchema targetSchema;
        private TranspileOptions options;
        private TranspileRuleRegistry registry;

        /**
         * Creates a transpiler builder.
         */
        public Builder() {
        }

        @Override
        public Builder sourceDialect(SqlDialectId sourceDialect) {
            this.sourceDialect = Objects.requireNonNull(sourceDialect, "sourceDialect");
            return this;
        }

        @Override
        public Builder targetDialect(SqlDialectId targetDialect) {
            this.targetDialect = Objects.requireNonNull(targetDialect, "targetDialect");
            return this;
        }

        @Override
        public Builder parser(Supplier<Specs> sourceSpecsFactory) {
            this.parserFactory = Objects.requireNonNull(sourceSpecsFactory, "sourceSpecsFactory");
            return this;
        }

        @Override
        public Builder renderer(Supplier<SqlDialect> targetDialectFactory) {
            this.rendererFactory = Objects.requireNonNull(targetDialectFactory, "targetDialectFactory");
            return this;
        }

        @Override
        public Builder targetValidation(Supplier<SchemaValidationSettings> targetValidationFactory) {
            this.validationFactory = Objects.requireNonNull(targetValidationFactory, "targetValidationFactory");
            return this;
        }

        @Override
        public Builder sourceSchema(CatalogSchema sourceSchema) {
            this.sourceSchema = sourceSchema;
            return this;
        }

        @Override
        public Builder targetSchema(CatalogSchema targetSchema) {
            this.targetSchema = targetSchema;
            return this;
        }

        @Override
        public Builder options(TranspileOptions options) {
            this.options = Objects.requireNonNull(options, "options");
            return this;
        }

        @Override
        public Builder registry(TranspileRuleRegistry registry) {
            this.registry = Objects.requireNonNull(registry, "registry");
            return this;
        }

        @Override
        public SqlTranspiler build() {
            if (sourceDialect == null) {
                throw new IllegalStateException("sourceDialect must be configured");
            }
            if (targetDialect == null) {
                throw new IllegalStateException("targetDialect must be configured");
            }
            return new DefaultSqlTranspiler(this);
        }
    }
}
