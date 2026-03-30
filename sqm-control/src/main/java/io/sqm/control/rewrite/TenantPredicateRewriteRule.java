package io.sqm.control.rewrite;

import io.sqm.control.decision.ReasonCode;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.pipeline.StatementRewriteResult;
import io.sqm.control.pipeline.StatementRewriteRule;
import io.sqm.control.pipeline.RewriteDenyException;
import io.sqm.core.*;
import io.sqm.core.transform.StatementTransforms;
import io.sqm.core.transform.VisibleTableBinding;

import java.util.*;
import java.util.function.Function;

/**
 * Built-in rewrite rule that injects tenant predicates into supported top-level statements.
 */
public final class TenantPredicateRewriteRule implements StatementRewriteRule {
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

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
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
     * Applies tenant predicate rewrite on supported top-level statements.
     *
     * @param statement parsed statement model
     * @param context execution context
     * @return rewrite result
     */
    @Override
    public StatementRewriteResult apply(Statement statement, ExecutionContext context) {
        Objects.requireNonNull(statement, "statement must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (settings.tenantTablePolicies().isEmpty()) {
            return StatementRewriteResult.unchanged(statement);
        }

        Statement rewritten = switch (statement) {
            case SelectQuery select -> rewriteSelect(select, context);
            case WithQuery with -> rewriteWith(with, context);
            case UpdateStatement update -> rewriteUpdate(update, context);
            case DeleteStatement delete -> rewriteDelete(delete, context);
            default -> statement;
        };

        if (rewritten == statement) {
            return StatementRewriteResult.unchanged(statement);
        }
        return StatementRewriteResult.rewritten(rewritten, id(), ReasonCode.REWRITE_TENANT_PREDICATE);
    }

    private Query rewriteWith(WithQuery with, ExecutionContext context) {
        if (!(with.body() instanceof SelectQuery body)) {
            return with;
        }
        Query rewrittenBody = rewriteSelect(body, context);
        return rewrittenBody == body ? with : with.body(rewrittenBody);
    }

    private Query rewriteSelect(SelectQuery select, ExecutionContext context) {
        return StatementTransforms.andWherePerTable(select, tenantResolver(select.where(), context));
    }

    private UpdateStatement rewriteUpdate(UpdateStatement update, ExecutionContext context) {
        return StatementTransforms.andWherePerTable(update, tenantResolver(update.where(), context));
    }

    private DeleteStatement rewriteDelete(DeleteStatement delete, ExecutionContext context) {
        return StatementTransforms.andWherePerTable(delete, tenantResolver(delete.where(), context));
    }

    private Function<VisibleTableBinding, Predicate> tenantResolver(Predicate where, ExecutionContext context) {
        String tenant = normalizeContextTenant(context.tenant());
        return binding -> {
            ResolvedPolicy resolved = resolvePolicy(binding.schema(), binding.tableName());
            if (resolved == null) {
                return null;
            }
            Target target = new Target(
                resolved.tableKey(),
                binding.qualifier(),
                resolved.policy().tenantColumn(),
                resolved.policy().mode()
            );
            if (tenant != null && isAlreadyConstrained(where, target, tenant)) {
                return null;
            }
            return switch (target.mode()) {
                case SKIP -> // Explicitly configured as no-op.
                    null;
                case OPTIONAL -> {
                    if (tenant != null) {
                        yield target.predicateForTenant(tenant);
                    }
                    yield null;
                }
                case REQUIRED -> {
                    if (tenant == null) {
                        throw new RewriteDenyException(
                            ReasonCode.DENY_TENANT_REQUIRED,
                            "Tenant context is required for tenant rewrite on table '%s'".formatted(target.tableKey())
                        );
                    }
                    yield target.predicateForTenant(tenant);
                }
            };
        };
    }

    private ResolvedPolicy resolvePolicy(String schema, String tableName) {
        var policies = settings.tenantTablePolicies();
        String name = normalize(tableName);

        if (schema != null) {
            String key = normalize(schema) + "." + name;
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

    private record Target(String tableKey, Identifier qualifier, String tenantColumn, TenantRewriteTableMode mode) {
        private Predicate predicateForTenant(String tenant) {
            return ColumnExpr.of(qualifier, Identifier.of(tenantColumn)).eq(Expression.literal(tenant));
        }
    }

    private record ResolvedPolicy(String tableKey, TenantRewriteTablePolicy policy) {
    }
}



