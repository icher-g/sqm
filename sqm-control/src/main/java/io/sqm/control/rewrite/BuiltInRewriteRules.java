package io.sqm.control.rewrite;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.BuiltInRewriteRule;
import io.sqm.control.BuiltInRewriteSettings;
import io.sqm.control.QueryRewriteRule;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Source of built-in {@link QueryRewriteRule} instances used by middleware rewrite pipelines.
 *
 * <p>This class does not construct {@code SqlQueryRewriter} instances directly. Callers select
 * baseline or subset rule packs through this type, then compose them into a rewriter via
 * {@code SqlQueryRewriter.chain(...)} or equivalent orchestration code.</p>
 */
public final class BuiltInRewriteRules {

    private static final EnumSet<BuiltInRewriteRule> AVAILABLE_RULES = EnumSet.of(
        BuiltInRewriteRule.LIMIT_INJECTION,
        BuiltInRewriteRule.IDENTIFIER_NORMALIZATION,
        BuiltInRewriteRule.CANONICALIZATION
    );

    private static final EnumSet<BuiltInRewriteRule> SCHEMA_AVAILABLE_RULES = EnumSet.of(
        BuiltInRewriteRule.LIMIT_INJECTION,
        BuiltInRewriteRule.SCHEMA_QUALIFICATION,
        BuiltInRewriteRule.COLUMN_QUALIFICATION,
        BuiltInRewriteRule.IDENTIFIER_NORMALIZATION,
        BuiltInRewriteRule.CANONICALIZATION
    );

    private BuiltInRewriteRules() {
    }

    /**
     * Returns all non-schema-aware built-in rewrite rules in deterministic enum order.
     *
     * @param settings built-in rewrite settings
     * @return immutable list of rewrite rules
     */
    public static List<QueryRewriteRule> allAvailable(BuiltInRewriteSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        return AVAILABLE_RULES.stream()
            .map(rule -> toRule(rule, settings, null))
            .toList();
    }

    /**
     * Returns selected non-schema-aware built-in rewrite rules in deterministic enum order.
        *
        * <p>Only the provided {@code rules} are returned. This method does not merge with the non-schema baseline
        * pack from {@link #allAvailable(BuiltInRewriteSettings)}.</p>
     *
     * @param settings built-in rewrite settings
     * @param rules selected built-in rule identifiers
     * @return immutable list of rewrite rules
     */
    public static List<QueryRewriteRule> selected(BuiltInRewriteSettings settings, Set<BuiltInRewriteRule> rules) {
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        ensureSupported(rules, AVAILABLE_RULES);

        return Arrays.stream(BuiltInRewriteRule.values())
            .filter(rules::contains)
            .map(rule -> toRule(rule, settings, null))
            .toList();
    }

    /**
     * Returns all schema-aware built-in rewrite rules in deterministic enum order.
     *
     * @param schema catalog schema used by schema-aware rules
     * @param settings built-in rewrite settings
     * @return immutable list of rewrite rules
     */
    public static List<QueryRewriteRule> allAvailable(CatalogSchema schema, BuiltInRewriteSettings settings) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        return SCHEMA_AVAILABLE_RULES.stream()
            .map(rule -> toRule(rule, settings, schema))
            .toList();
    }

    /**
     * Returns selected schema-aware built-in rewrite rules in deterministic enum order.
        *
        * <p>Only the provided {@code rules} are returned. This method does not merge with the schema-aware baseline
        * pack from {@link #allAvailable(CatalogSchema, BuiltInRewriteSettings)}.</p>
     *
     * @param schema catalog schema used by schema-aware rules
     * @param settings built-in rewrite settings
     * @param rules selected built-in rule identifiers
     * @return immutable list of rewrite rules
     */
    public static List<QueryRewriteRule> selected(CatalogSchema schema, BuiltInRewriteSettings settings, Set<BuiltInRewriteRule> rules) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        ensureSupported(rules, SCHEMA_AVAILABLE_RULES);

        return Arrays.stream(BuiltInRewriteRule.values())
            .filter(rules::contains)
            .map(rule -> toRule(rule, settings, schema))
            .toList();
    }

    private static void ensureSupported(Set<BuiltInRewriteRule> selected, Set<BuiltInRewriteRule> supported) {
        if (!supported.containsAll(selected)) {
            EnumSet<BuiltInRewriteRule> unsupported = EnumSet.copyOf(selected);
            unsupported.removeAll(supported);
            throw new IllegalArgumentException("unsupported built-in rewrites requested: " + unsupported);
        }
    }

    private static QueryRewriteRule toRule(BuiltInRewriteRule rule, BuiltInRewriteSettings settings, CatalogSchema schema) {
        return switch (rule) {
            case LIMIT_INJECTION -> LimitInjectionRewriteRule.of(settings);
            case SCHEMA_QUALIFICATION -> SchemaQualificationRewriteRule.of(
                Objects.requireNonNull(schema, "schema must be provided for SCHEMA_QUALIFICATION"),
                settings);
            case COLUMN_QUALIFICATION -> ColumnQualificationRewriteRule.of(
                Objects.requireNonNull(schema, "schema must be provided for COLUMN_QUALIFICATION"),
                settings);
            case IDENTIFIER_NORMALIZATION -> IdentifierNormalizationRewriteRule.of(settings);
            case CANONICALIZATION -> CanonicalizationRewriteRule.of();
        };
    }
}
