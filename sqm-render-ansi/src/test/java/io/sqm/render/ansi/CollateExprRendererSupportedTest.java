package io.sqm.render.ansi;

import io.sqm.core.Direction;
import io.sqm.core.Expression;
import io.sqm.core.Nulls;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.render.defaults.DefaultOperators;
import io.sqm.render.defaults.DefaultValueFormatter;
import io.sqm.render.repos.DefaultRenderersRepository;
import io.sqm.render.spi.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renderer tests for expression-level COLLATE when feature is enabled.
 */
class CollateExprRendererSupportedTest {

    @Test
    void renders_collate_expression_with_feature_enabled() {
        var dialect = new TestDialect();
        dialect.renderers()
            .register(new ColumnRefRenderer())
            .register(new CollateExprRenderer());

        RenderContext ctx = RenderContext.of(dialect);
        Expression expr = col("name").collate("de-CH");

        String sql = ctx.render(expr).sql();
        assertTrue(sql.contains("COLLATE \"de-CH\""));
    }

    /**
     * Minimal dialect with expression-level COLLATE enabled for renderer tests.
     */
    private static final class TestDialect implements SqlDialect {

        private final RenderersRepository renderers = new DefaultRenderersRepository();
        private final IdentifierQuoter quoter = new TestQuoter();
        private final Operators operators = new DefaultOperators();
        private final Booleans booleans = new TestBooleans();
        private final NullSorting nullSorting = new TestNullSorting();
        private final PaginationStyle paginationStyle = new TestPaginationStyle();
        private final ValueFormatter formatter = new DefaultValueFormatter(this);
        private final DialectCapabilities capabilities = VersionedDialectCapabilities.builder(SqlDialectVersion.minimum())
            .supports(SqlFeature.EXPR_COLLATE)
            .build();

        /**
         * Returns dialect name.
         *
         * @return dialect name
         */
        @Override
        public String name() {
            return "test";
        }

        /**
         * Returns identifier quoter.
         *
         * @return identifier quoter
         */
        @Override
        public IdentifierQuoter quoter() {
            return quoter;
        }

        /**
         * Returns value formatter.
         *
         * @return value formatter
         */
        @Override
        public ValueFormatter formatter() {
            return formatter;
        }

        /**
         * Returns operators registry.
         *
         * @return operators registry
         */
        @Override
        public Operators operators() {
            return operators;
        }

        /**
         * Returns boolean literal strategy.
         *
         * @return booleans strategy
         */
        @Override
        public Booleans booleans() {
            return booleans;
        }

        /**
         * Returns null sorting strategy.
         *
         * @return null sorting strategy
         */
        @Override
        public NullSorting nullSorting() {
            return nullSorting;
        }

        /**
         * Returns pagination style.
         *
         * @return pagination style
         */
        @Override
        public PaginationStyle paginationStyle() {
            return paginationStyle;
        }

        /**
         * Returns dialect capabilities.
         *
         * @return dialect capabilities
         */
        @Override
        public DialectCapabilities capabilities() {
            return capabilities;
        }

        /**
         * Returns renderers repository.
         *
         * @return renderers repository
         */
        @Override
        public RenderersRepository renderers() {
            return renderers;
        }

        private static final class TestQuoter implements IdentifierQuoter {
            /**
             * Quotes identifier.
             *
             * @param identifier identifier to quote
             * @return quoted identifier
             */
            @Override
            public String quote(String identifier) {
                return "\"" + identifier + "\"";
            }

            /**
             * Quotes identifier if needed.
             *
             * @param identifier identifier to quote
             * @return quoted identifier
             */
            @Override
            public String quoteIfNeeded(String identifier) {
                return quote(identifier);
            }

            /**
             * Qualifies an identifier with schema when provided.
             *
             * @param schemaOrNull optional schema
             * @param name         identifier name
             * @return qualified identifier
             */
            @Override
            public String qualify(String schemaOrNull, String name) {
                if (schemaOrNull == null || schemaOrNull.isBlank()) {
                    return quote(name);
                }
                return quote(schemaOrNull) + "." + quote(name);
            }

            /**
             * Reports whether identifier needs quoting.
             *
             * @param identifier identifier to check
             * @return true if quoting needed
             */
            @Override
            public boolean needsQuoting(String identifier) {
                return true;
            }
        }

        private static final class TestBooleans implements Booleans {
            /**
             * Returns TRUE literal.
             *
             * @return TRUE literal
             */
            @Override
            public String trueLiteral() {
                return "TRUE";
            }

            /**
             * Returns FALSE literal.
             *
             * @return FALSE literal
             */
            @Override
            public String falseLiteral() {
                return "FALSE";
            }

            /**
             * Indicates whether predicates require explicit comparison.
             *
             * @return false
             */
            @Override
            public boolean requireExplicitPredicate() {
                return false;
            }
        }

        private static final class TestNullSorting implements NullSorting {
            /**
             * Indicates whether explicit syntax is supported by the dialect.
             *
             * @return True if the explicit syntax is supported or False otherwise.
             */
            @Override
            public boolean supportsExplicit() {
                return false;
            }

            /**
             * Gets a string representation of a {@link Nulls} enum value.
             * <p>Example:</p>
             * <pre>
             *     {@code
             *     NULLS FIRST | NULLS LAST
             *     }
             * </pre>
             *
             * @param n an enum value.
             * @return a string representation of "\"s\".\"t\"" | "[s].[t]".
             */
            @Override
            public String keyword(Nulls n) {
                return "";
            }

            /**
             * Gets a default {@link Nulls} value for a {@link Direction} in a dialect.
             * <p>Example:</p>
             * <pre>
             *     {@code
             *     ASC -> LAST, DESC -> FIRST (typical)
             *     }
             * </pre>
             *
             * @param dir a direction to supply the default for.
             * @return a default.
             */
            @Override
            public Nulls defaultFor(Direction dir) {
                return null;
            }
        }

        private static final class TestPaginationStyle implements PaginationStyle {
            /**
             * Indicates if the dialect supports limit and offset.
             *
             * @return True if supported and False otherwise.
             */
            @Override
            public boolean supportsLimitOffset() {
                return false;
            }

            /**
             * Indicates if the dialect supports offset fetch.
             *
             * @return True if supported and False otherwise.
             */
            @Override
            public boolean supportsOffsetFetch() {
                return false;
            }

            /**
             * Indicates if the dialect supports top.
             *
             * @return True if supported and False otherwise.
             */
            @Override
            public boolean supportsTop() {
                return false;
            }
        }
    }
}
