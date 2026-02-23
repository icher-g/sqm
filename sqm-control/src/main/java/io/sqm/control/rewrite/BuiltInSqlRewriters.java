package io.sqm.control.rewrite;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.BuiltInRewriteRule;
import io.sqm.control.BuiltInRewriteSettings;
import io.sqm.control.QueryRewriteRule;
import io.sqm.control.SqlQueryRewriter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Factory utilities for selecting built-in middleware rewrite pipelines.
 *
 * <p>This class provides an easy-to-use entry point for future built-in rewrite packs
 * while preserving explicit control through {@link SqlQueryRewriter}.</p>
 */
public final class BuiltInSqlRewriters {
    private static final EnumSet<BuiltInRewriteRule> AVAILABLE_RULES = EnumSet.of(
        BuiltInRewriteRule.LIMIT_INJECTION,
        BuiltInRewriteRule.CANONICALIZATION
    );
    private static final EnumSet<BuiltInRewriteRule> SCHEMA_AVAILABLE_RULES = EnumSet.of(
        BuiltInRewriteRule.LIMIT_INJECTION,
        BuiltInRewriteRule.SCHEMA_QUALIFICATION,
        BuiltInRewriteRule.COLUMN_QUALIFICATION,
        BuiltInRewriteRule.CANONICALIZATION
    );

    private BuiltInSqlRewriters() {
    }

    /**
     * Returns the set of built-in rewrite rules currently implemented and available.
     *
     * @return immutable set of available rules.
     */
    public static Set<BuiltInRewriteRule> availableRules() {
        return Set.copyOf(AVAILABLE_RULES);
    }

    /**
     * Returns a rewriter composed from all currently available built-in rules.
     *
     * @return built-in rewriter pipeline, or no-op when no built-in rules are available yet.
     */
    public static SqlQueryRewriter allAvailable() {
        return allAvailable(BuiltInRewriteSettings.defaults());
    }

    /**
     * Returns a rewriter composed from all currently available built-in rules using explicit settings.
     *
     * @param settings built-in rewrite settings
     * @return built-in rewriter pipeline
     */
    public static SqlQueryRewriter allAvailable(BuiltInRewriteSettings settings) {
        return compose(AVAILABLE_RULES, settings, null);
    }

    /**
     * Returns a schema-aware rewriter composed from all built-in rules available with catalog metadata.
     *
     * @param schema catalog schema used for schema qualification
     * @return built-in rewriter pipeline
     */
    public static SqlQueryRewriter allAvailable(CatalogSchema schema) {
        return allAvailable(schema, BuiltInRewriteSettings.defaults());
    }

    /**
     * Returns a schema-aware rewriter composed from all built-in rules available with catalog metadata.
     *
     * @param schema catalog schema used for schema qualification
     * @param settings built-in rewrite settings
     * @return built-in rewriter pipeline
     */
    public static SqlQueryRewriter allAvailable(CatalogSchema schema, BuiltInRewriteSettings settings) {
        Objects.requireNonNull(schema, "schema must not be null");
        return compose(SCHEMA_AVAILABLE_RULES, settings, schema);
    }

    /**
     * Returns a rewriter composed from the provided built-in rewrite rules.
     *
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter of(BuiltInRewriteRule... rules) {
        return of(BuiltInRewriteSettings.defaults(), rules);
    }

    /**
     * Returns a rewriter composed from the provided built-in rewrite rules using explicit settings.
     *
     * @param settings built-in rewrite settings
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter of(BuiltInRewriteSettings settings, BuiltInRewriteRule... rules) {
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        if (rules.length == 0) {
            return SqlQueryRewriter.noop();
        }
        return of(settings, EnumSet.copyOf(Arrays.asList(rules)));
    }

    /**
     * Returns a rewriter composed from the provided built-in rewrite rules.
     *
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter of(Set<BuiltInRewriteRule> rules) {
        return of(BuiltInRewriteSettings.defaults(), rules);
    }

    /**
     * Returns a rewriter composed from the provided built-in rewrite rules using explicit settings.
     *
     * @param settings built-in rewrite settings
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter of(BuiltInRewriteSettings settings, Set<BuiltInRewriteRule> rules) {
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        if (rules.isEmpty()) {
            return SqlQueryRewriter.noop();
        }
        EnumSet<BuiltInRewriteRule> selected = EnumSet.copyOf(rules);
        return of(settings, selected);
    }

    /**
     * Returns a schema-aware rewriter composed from provided built-in rewrite rules.
     *
     * @param schema catalog schema used for schema qualification
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter forSchema(CatalogSchema schema, BuiltInRewriteRule... rules) {
        return forSchema(schema, BuiltInRewriteSettings.defaults(), rules);
    }

    /**
     * Returns a schema-aware rewriter composed from provided built-in rewrite rules using explicit settings.
     *
     * @param schema catalog schema used for schema qualification
     * @param settings built-in rewrite settings
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter forSchema(CatalogSchema schema, BuiltInRewriteSettings settings, BuiltInRewriteRule... rules) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        if (rules.length == 0) {
            return SqlQueryRewriter.noop();
        }
        return forSchema(schema, settings, EnumSet.copyOf(Arrays.asList(rules)));
    }

    /**
     * Returns a schema-aware rewriter composed from provided built-in rewrite rules using explicit settings.
     *
     * @param schema catalog schema used for schema qualification
     * @param settings built-in rewrite settings
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter forSchema(CatalogSchema schema, BuiltInRewriteSettings settings, Set<BuiltInRewriteRule> rules) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        if (rules.isEmpty()) {
            return SqlQueryRewriter.noop();
        }
        EnumSet<BuiltInRewriteRule> selected = EnumSet.copyOf(rules);
        ensureSupported(selected, SCHEMA_AVAILABLE_RULES);
        return compose(selected, settings, schema);
    }

    private static SqlQueryRewriter of(BuiltInRewriteSettings settings, EnumSet<BuiltInRewriteRule> selected) {
        ensureSupported(selected, AVAILABLE_RULES);
        return compose(selected, settings, null);
    }

    private static void ensureSupported(Set<BuiltInRewriteRule> selected, Set<BuiltInRewriteRule> supported) {
        if (!supported.containsAll(selected)) {
            EnumSet<BuiltInRewriteRule> unsupported = EnumSet.copyOf(selected);
            unsupported.removeAll(supported);
            throw new IllegalArgumentException("unsupported built-in rewrites requested: " + unsupported);
        }
    }

    private static SqlQueryRewriter compose(Set<BuiltInRewriteRule> rules, BuiltInRewriteSettings settings, CatalogSchema schema) {
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        if (rules.isEmpty()) {
            return SqlQueryRewriter.noop();
        }

        List<QueryRewriteRule> orderedRules = Arrays.stream(BuiltInRewriteRule.values())
            .filter(rules::contains)
            .map(rule -> toRule(rule, settings, schema))
            .toList();
        return SqlQueryRewriter.chain(orderedRules);
    }

    private static QueryRewriteRule toRule(BuiltInRewriteRule rule, BuiltInRewriteSettings settings, CatalogSchema schema) {
        return switch (rule) {
            case LIMIT_INJECTION -> LimitInjectionRewriteRule.of(settings);
            case SCHEMA_QUALIFICATION -> SchemaQualificationRewriteRule.of(Objects.requireNonNull(
                schema,
                "schema must be provided for SCHEMA_QUALIFICATION"
            ), settings);
            case COLUMN_QUALIFICATION -> ColumnQualificationRewriteRule.of(Objects.requireNonNull(
                schema,
                "schema must be provided for COLUMN_QUALIFICATION"
            ), settings);
            case CANONICALIZATION -> CanonicalizationRewriteRule.of();
            default -> throw new IllegalArgumentException("unsupported built-in rewrites requested: " + Set.of(rule));
        };
    }
}
