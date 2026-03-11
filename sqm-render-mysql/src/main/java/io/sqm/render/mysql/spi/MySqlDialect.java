package io.sqm.render.mysql.spi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.mysql.dialect.MySqlCapabilities;
import io.sqm.render.defaults.DefaultValueFormatter;
import io.sqm.render.mysql.Renderers;
import io.sqm.render.ansi.spi.AnsiBooleans;
import io.sqm.render.ansi.spi.AnsiNullSorting;
import io.sqm.render.spi.*;

import java.util.Objects;

/**
 * MySQL SQL rendering dialect implementation.
 */
public class MySqlDialect implements SqlDialect {

    private final IdentifierQuoter quoter;
    private final ValueFormatter formatter = new DefaultValueFormatter(this);
    private final Operators operators = new MySqlOperators();
    private final Booleans booleans = new AnsiBooleans();
    private final NullSorting nullSorting = new AnsiNullSorting();
    private final PaginationStyle paginationStyle = new MySqlPaginationStyle();
    private final RenderersRepository repository = Renderers.mysql();
    private final DialectCapabilities capabilities;
    private final MySqlOptimizerHintNormalizationPolicy optimizerHintNormalizationPolicy;

    /**
     * Creates a MySQL dialect for baseline version 8.0 with backtick identifier quoting.
     */
    public MySqlDialect() {
        this(SqlDialectVersion.of(8, 0), false, MySqlOptimizerHintNormalizationPolicy.PASS_THROUGH);
    }

    /**
     * Creates a MySQL dialect for a specific version with backtick identifier quoting.
     *
     * @param version MySQL version used to evaluate feature availability.
     */
    public MySqlDialect(SqlDialectVersion version) {
        this(version, false, MySqlOptimizerHintNormalizationPolicy.PASS_THROUGH);
    }

    /**
     * Creates a MySQL dialect for a specific version with explicit
     * optimizer-hint normalization policy.
     *
     * @param version MySQL version used to evaluate feature availability.
     * @param optimizerHintNormalizationPolicy optimizer-hint normalization policy.
     */
    public MySqlDialect(
        SqlDialectVersion version,
        MySqlOptimizerHintNormalizationPolicy optimizerHintNormalizationPolicy
    ) {
        this(version, false, optimizerHintNormalizationPolicy);
    }

    /**
     * Creates a MySQL dialect for a specific version and quote mode.
     *
     * @param version        MySQL version used to evaluate feature availability.
     * @param ansiQuotesMode if {@code true}, double-quote identifiers are supported by the quoter.
     */
    public MySqlDialect(SqlDialectVersion version, boolean ansiQuotesMode) {
        this(version, ansiQuotesMode, MySqlOptimizerHintNormalizationPolicy.PASS_THROUGH);
    }

    /**
     * Creates a MySQL dialect for a specific version, quote mode, and
     * optimizer-hint normalization policy.
     *
     * @param version MySQL version used to evaluate feature availability.
     * @param ansiQuotesMode if {@code true}, double-quote identifiers are supported by the quoter.
     * @param optimizerHintNormalizationPolicy optimizer-hint normalization policy.
     */
    public MySqlDialect(
        SqlDialectVersion version,
        boolean ansiQuotesMode,
        MySqlOptimizerHintNormalizationPolicy optimizerHintNormalizationPolicy
    ) {
        this.capabilities = MySqlCapabilities.of(version);
        this.quoter = new MySqlIdentifierQuoter(ansiQuotesMode);
        this.optimizerHintNormalizationPolicy = Objects.requireNonNull(optimizerHintNormalizationPolicy, "optimizerHintNormalizationPolicy");
    }

    /**
     * Returns the dialect name.
     *
     * @return dialect name.
     */
    @Override
    public String name() {
        return "MySQL";
    }

    /**
     * Returns identifier quoter.
     *
     * @return identifier quoter.
     */
    @Override
    public IdentifierQuoter quoter() {
        return quoter;
    }

    /**
     * Returns value formatter.
     *
     * @return value formatter.
     */
    @Override
    public ValueFormatter formatter() {
        return formatter;
    }

    /**
     * Returns operators configuration.
     *
     * @return operators configuration.
     */
    @Override
    public Operators operators() {
        return operators;
    }

    /**
     * Returns booleans configuration.
     *
     * @return booleans configuration.
     */
    @Override
    public Booleans booleans() {
        return booleans;
    }

    /**
     * Returns null sorting configuration.
     *
     * @return null sorting configuration.
     */
    @Override
    public NullSorting nullSorting() {
        return nullSorting;
    }

    /**
     * Returns pagination style.
     *
     * @return pagination style.
     */
    @Override
    public PaginationStyle paginationStyle() {
        return paginationStyle;
    }

    /**
     * Returns MySQL feature capabilities.
     *
     * @return feature capabilities.
     */
    @Override
    public DialectCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Returns optimizer-hint normalization policy used during rendering.
     *
     * @return optimizer-hint normalization policy.
     */
    public MySqlOptimizerHintNormalizationPolicy optimizerHintNormalizationPolicy() {
        return optimizerHintNormalizationPolicy;
    }

    /**
     * Returns renderers repository.
     *
     * @return renderers repository.
     */
    @Override
    public RenderersRepository renderers() {
        return repository;
    }
}


