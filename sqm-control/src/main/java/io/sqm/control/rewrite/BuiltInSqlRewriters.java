package io.sqm.control.rewrite;

import io.sqm.control.BuiltInRewriteRule;
import io.sqm.control.SqlQueryRewriter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Factory utilities for selecting built-in middleware rewrite pipelines.
 *
 * <p>This class provides an easy-to-use entry point for future built-in rewrite packs
 * while preserving explicit control through {@link SqlQueryRewriter}.</p>
 */
public final class BuiltInSqlRewriters {
    private static final EnumSet<BuiltInRewriteRule> AVAILABLE_RULES = EnumSet.noneOf(BuiltInRewriteRule.class);

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
        return compose(AVAILABLE_RULES);
    }

    /**
     * Returns a rewriter composed from the provided built-in rewrite rules.
     *
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter of(BuiltInRewriteRule... rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        if (rules.length == 0) {
            return SqlQueryRewriter.noop();
        }
        return of(EnumSet.copyOf(Arrays.asList(rules)));
    }

    /**
     * Returns a rewriter composed from the provided built-in rewrite rules.
     *
     * @param rules built-in rewrite rules
     * @return composed rewriter
     */
    public static SqlQueryRewriter of(Set<BuiltInRewriteRule> rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        if (rules.isEmpty()) {
            return SqlQueryRewriter.noop();
        }
        EnumSet<BuiltInRewriteRule> selected = EnumSet.copyOf(rules);
        if (!AVAILABLE_RULES.containsAll(selected)) {
            EnumSet<BuiltInRewriteRule> unsupported = EnumSet.copyOf(selected);
            unsupported.removeAll(AVAILABLE_RULES);
            throw new IllegalArgumentException("unsupported built-in rewrites requested: " + unsupported);
        }
        return compose(selected);
    }

    private static SqlQueryRewriter compose(Set<BuiltInRewriteRule> rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        // Built-in engine rewrites are not wired yet in this iteration.
        // This remains a no-op until concrete rule adapters are implemented.
        return SqlQueryRewriter.noop();
    }
}
