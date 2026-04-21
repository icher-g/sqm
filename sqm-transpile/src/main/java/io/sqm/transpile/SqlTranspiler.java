package io.sqm.transpile;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.parser.spi.Specs;
import io.sqm.render.spi.SqlDialect;
import io.sqm.transpile.rule.TranspileRuleRegistry;
import io.sqm.validate.schema.SchemaValidationSettings;

import java.util.function.Supplier;

/**
 * Transpiles SQL statements from one dialect to another.
 */
public interface SqlTranspiler {
    /**
     * Creates a transpiler builder.
     *
     * @return builder
     */
    static Builder builder() {
        return new DefaultSqlTranspiler.Builder();
    }

    /**
     * Transpiles source SQL text.
     *
     * @param sql source SQL text
     * @return transpilation result
     */
    TranspileResult transpile(String sql);

    /**
     * Transpiles an already parsed statement.
     *
     * @param statement source statement model
     * @return transpilation result
     */
    TranspileResult transpile(Statement statement);

    /**
     * Transpiles an already parsed statement sequence.
     *
     * @param sequence source statement sequence model
     * @return transpilation result
     */
    TranspileResult transpile(StatementSequence sequence);

    /**
     * Builder for {@link SqlTranspiler} instances.
     */
    interface Builder {
        /**
         * Sets the source dialect identifier.
         *
         * @param sourceDialect source dialect identifier
         * @return this builder
         */
        Builder sourceDialect(SqlDialectId sourceDialect);

        /**
         * Sets the target dialect identifier.
         *
         * @param targetDialect target dialect identifier
         * @return this builder
         */
        Builder targetDialect(SqlDialectId targetDialect);

        /**
         * Sets the source parser specs factory.
         *
         * @param sourceSpecsFactory source parser specs factory
         * @return this builder
         */
        Builder parser(Supplier<Specs> sourceSpecsFactory);

        /**
         * Sets the target renderer dialect factory.
         *
         * @param targetDialectFactory target renderer dialect factory
         * @return this builder
         */
        Builder renderer(Supplier<SqlDialect> targetDialectFactory);

        /**
         * Sets the target validation settings factory.
         *
         * @param targetValidationFactory target validation settings factory
         * @return this builder
         */
        Builder targetValidation(Supplier<SchemaValidationSettings> targetValidationFactory);

        /**
         * Sets the optional source schema.
         *
         * @param sourceSchema source schema
         * @return this builder
         */
        Builder sourceSchema(CatalogSchema sourceSchema);

        /**
         * Sets the optional target schema.
         *
         * @param targetSchema target schema
         * @return this builder
         */
        Builder targetSchema(CatalogSchema targetSchema);

        /**
         * Sets transpilation options.
         *
         * @param options transpilation options
         * @return this builder
         */
        Builder options(TranspileOptions options);

        /**
         * Sets the transpilation rule registry.
         *
         * @param registry transpilation rule registry
         * @return this builder
         */
        Builder registry(TranspileRuleRegistry registry);

        /**
         * Builds the transpiler instance.
         *
         * @return transpiler instance
         */
        SqlTranspiler build();
    }
}
