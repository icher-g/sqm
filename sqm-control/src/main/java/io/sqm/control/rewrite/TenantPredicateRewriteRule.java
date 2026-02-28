package io.sqm.control.rewrite;

import io.sqm.control.*;
import io.sqm.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Built-in rewrite rule that injects tenant predicates into top-level {@link SelectQuery} table sources.
 */
public final class TenantPredicateRewriteRule implements QueryRewriteRule {
    private static final String RULE_ID = "tenant-predicate";

    private final BuiltInRewriteSettings settings;

    private TenantPredicateRewriteRule(BuiltInRewriteSettings settings) {
        this.settings = settings;
    }

    /**
     * Creates a tenant predicate rewrite rule from built-in rewrite settings.
     *
     * @param settings built-in rewrite settings
     * @return tenant predicate rewrite rule
     */
    public static TenantPredicateRewriteRule of(BuiltInRewriteSettings settings) {
        return new TenantPredicateRewriteRule(Objects.requireNonNull(settings, "settings must not be null"));
    }

    /**
     * Returns stable rewrite-rule id.
     *
     * @return rule identifier
     */
    @Override
    public String id() {
        return RULE_ID;
    }

    /**
     * Applies tenant predicate rewrite on top-level {@link SelectQuery} only.
     *
     * @param query parsed query model
     * @param context execution context
     * @return rewrite result
     */
    @Override
    public QueryRewriteResult apply(Query query, ExecutionContext context) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (settings.tenantTablePolicies().isEmpty()) {
            return QueryRewriteResult.unchanged(query);
        }

        Query rewritten = switch (query) {
            case SelectQuery select -> rewriteSelect(select, context);
            case WithQuery with -> rewriteWith(with, context);
            default -> query;
        };

        if (rewritten == query) {
            return QueryRewriteResult.unchanged(query);
        }
        return QueryRewriteResult.rewritten(rewritten, id(), ReasonCode.REWRITE_TENANT_PREDICATE);
    }

    private Query rewriteWith(WithQuery with, ExecutionContext context) {
        if (!(with.body() instanceof SelectQuery body)) {
            return with;
        }
        Query rewrittenBody = rewriteSelect(body, context);
        return rewrittenBody == body ? with : with.body(rewrittenBody);
    }

    private Query rewriteSelect(SelectQuery select, ExecutionContext context) {
        List<Target> targets = collectTargets(select);
        if (targets.isEmpty()) {
            return select;
        }

        String tenant = normalizeContextTenant(context.tenant());
        Predicate tenantPredicate = null;
        for (Target target : targets) {
            if (tenant != null && isAlreadyConstrained(select.where(), target, tenant)) {
                continue;
            }
            switch (target.mode()) {
                case SKIP -> {
                    // Explicitly configured as no-op.
                }
                case OPTIONAL -> {
                    if (tenant != null) {
                        tenantPredicate = and(tenantPredicate, target.predicateForTenant(tenant));
                    }
                }
                case REQUIRED -> {
                    if (tenant == null) {
                        throw new RewriteDenyException(
                            ReasonCode.DENY_TENANT_REQUIRED,
                            "Tenant context is required for tenant rewrite on table '%s'".formatted(target.tableKey())
                        );
                    }
                    tenantPredicate = and(tenantPredicate, target.predicateForTenant(tenant));
                }
            }
        }

        if (tenantPredicate == null) {
            return select;
        }

        Predicate where = select.where() == null ? tenantPredicate : select.where().and(tenantPredicate);
        return SelectQueryBuilder.of(select)
            .where(where)
            .build();
    }

    private static boolean isAlreadyConstrained(Predicate where, Target target, String tenant) {
        if (where == null) {
            return false;
        }
        for (Predicate conjunct : conjunctionTerms(where)) {
            if (isTenantEqualityPredicate(conjunct, target, tenant)) {
                return true;
            }
        }
        return false;
    }

    private static List<Predicate> conjunctionTerms(Predicate predicate) {
        if (predicate instanceof AndPredicate and) {
            var terms = new ArrayList<Predicate>();
            terms.addAll(conjunctionTerms(and.lhs()));
            terms.addAll(conjunctionTerms(and.rhs()));
            return terms;
        }
        return List.of(predicate);
    }

    private static boolean isTenantEqualityPredicate(Predicate predicate, Target target, String tenant) {
        if (!(predicate instanceof ComparisonPredicate comparison)) {
            return false;
        }
        if (comparison.operator() != ComparisonOperator.EQ) {
            return false;
        }
        return matchesColumnToTenantLiteral(comparison.lhs(), comparison.rhs(), target, tenant)
            || matchesColumnToTenantLiteral(comparison.rhs(), comparison.lhs(), target, tenant);
    }

    private static boolean matchesColumnToTenantLiteral(
        Expression maybeColumn,
        Expression maybeLiteral,
        Target target,
        String tenant
    ) {
        if (!(maybeColumn instanceof ColumnExpr column)) {
            return false;
        }
        if (!(maybeLiteral instanceof LiteralExpr literal) || !(literal.value() instanceof String value)) {
            return false;
        }
        if (!Objects.equals(value, tenant)) {
            return false;
        }
        if (!equalsIgnoreCase(column.name().value(), target.tenantColumn())) {
            return false;
        }
        if (column.tableAlias() == null) {
            return false;
        }
        return equalsIgnoreCase(column.tableAlias().value(), target.qualifier().value());
    }

    private List<Target> collectTargets(SelectQuery select) {
        var targets = new ArrayList<Target>();
        addTargetFromTableRef(targets, select.from());
        for (Join join : select.joins()) {
            addTargetFromTableRef(targets, join.right());
        }
        return List.copyOf(targets);
    }

    private void addTargetFromTableRef(List<Target> targets, TableRef ref) {
        if (ref == null) {
            return;
        }
        if (ref instanceof Lateral lateral) {
            addTargetFromTableRef(targets, lateral.inner());
            return;
        }
        if (!(ref instanceof Table table)) {
            return;
        }

        ResolvedPolicy resolved = resolvePolicy(table);
        if (resolved == null) {
            return;
        }
        Identifier qualifier = table.alias() != null ? table.alias() : table.name();
        targets.add(new Target(
            resolved.tableKey(),
            qualifier,
            resolved.policy().tenantColumn(),
            resolved.policy().mode()
        ));
    }

    private ResolvedPolicy resolvePolicy(Table table) {
        var policies = settings.tenantTablePolicies();
        String name = normalize(table.name().value());

        if (table.schema() != null) {
            String key = tableKey(table.schema(), table.name());
            TenantRewriteTablePolicy policy = policies.get(key);
            return onMissingMapping(key, policy);
        }

        String preferredSchema = normalize(settings.qualificationDefaultSchema());
        if (preferredSchema != null) {
            String preferredKey = preferredSchema + "." + name;
            TenantRewriteTablePolicy preferred = policies.get(preferredKey);
            if (preferred != null) {
                return new ResolvedPolicy(preferredKey, preferred);
            }
        }

        List<Map.Entry<String, TenantRewriteTablePolicy>> matches = policies.entrySet()
            .stream()
            .filter(e -> e.getKey().endsWith("." + name))
            .toList();

        if (matches.isEmpty()) {
            return onMissingMapping(name, null);
        }
        if (matches.size() == 1) {
            var match = matches.getFirst();
            return new ResolvedPolicy(match.getKey(), match.getValue());
        }
        if (settings.tenantAmbiguityMode() == TenantRewriteAmbiguityMode.DENY) {
            throw new RewriteDenyException(
                ReasonCode.DENY_TENANT_MAPPING_AMBIGUOUS,
                "Ambiguous tenant mapping for unqualified table '%s'".formatted(name)
            );
        }
        return null;
    }

    private ResolvedPolicy onMissingMapping(String tableKey, TenantRewriteTablePolicy policy) {
        if (policy != null) {
            return new ResolvedPolicy(tableKey, policy);
        }
        if (settings.tenantFallbackMode() == TenantRewriteFallbackMode.DENY) {
            throw new RewriteDenyException(
                ReasonCode.DENY_TENANT_MAPPING_MISSING,
                "Missing tenant mapping for table '%s'".formatted(tableKey)
            );
        }
        return null;
    }

    private static String tableKey(Identifier schema, Identifier table) {
        return normalize(schema.value()) + "." + normalize(table.value());
    }

    private static String normalizeContextTenant(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return null;
        }
        return tenant;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static Predicate and(Predicate lhs, Predicate rhs) {
        return lhs == null ? rhs : lhs.and(rhs);
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    private record Target(String tableKey, Identifier qualifier, String tenantColumn, TenantRewriteTableMode mode) {
        private Predicate predicateForTenant(String tenant) {
            return ColumnExpr.of(qualifier, Identifier.of(tenantColumn)).eq(Expression.literal(tenant));
        }
    }

    private record ResolvedPolicy(String tableKey, TenantRewriteTablePolicy policy) {
    }
}
