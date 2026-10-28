package io.sqm.validate.oracle.rule;

import io.sqm.core.*;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.schema.internal.SchemaValidationContext;
import io.sqm.validate.schema.rule.SchemaValidationRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates Oracle MODEL value restrictions without coupling parsing or rendering
 * to semantic validation. This rule does not perform MODEL name/type or
 * correlation resolution.
 */
public final class OracleModelValidationRule implements SchemaValidationRule<SelectQuery> {
    private static final BigDecimal MAX_CELLS = BigDecimal.valueOf(10000);

    /**
     * Creates an Oracle MODEL value-validation rule.
     */
    public OracleModelValidationRule() {
    }

    private static void validateTarget(CellTarget target, String path, SchemaValidationContext context) {
        BigDecimal combinations = BigDecimal.ONE;
        for (var index = 0; index < target.addresses().size(); index++) {
            var address = target.addresses().get(index);
            if (!(address instanceof CellFor generator)) continue;
            var addressPath = path + ".addresses[" + index + "]";
            var count = generator instanceof CellFor.Values values
                ? validateValues(values, addressPath, context)
                : validateRange((CellFor.Range) generator, addressPath, context);
            // Continue checking later generators even when one count is unknown.
            combinations = combinations == null || count == null ? null : combinations.multiply(count);
        }
        if (combinations != null && combinations.compareTo(MAX_CELLS) > 0) {
            invalid(context, target, "MODEL FOR target cannot generate more than 10000 combinations", path);
        }
    }

    private static BigDecimal validateValues(CellFor.Values values, String path, SchemaValidationContext context) {
        var count = switch (values.values()) {
            case RowExpr row -> BigDecimal.valueOf(row.items().size());
            case RowListExpr rows -> BigDecimal.valueOf(rows.rows().size());
            case QueryExpr query -> {
                validateWidth(query.subquery(), values.dimensions().size(), path + ".values", context);
                yield null;
            }
        };
        if (count != null && count.compareTo(MAX_CELLS) > 0) {
            invalid(context, values, "MODEL FOR cannot generate more than 10000 values or tuples", path + ".values");
            return null;
        }
        return count;
    }

    private static BigDecimal validateRange(CellFor.Range range, String path, SchemaValidationContext context) {
        if (range.likePattern() instanceof LiteralExpr literal
            && (!(literal.value() instanceof String pattern) || pattern.chars().filter(c -> c == '%').count() != 1)) {
            invalid(context, range.likePattern(), "MODEL FOR LIKE template must contain exactly one %", path + ".likePattern");
        }
        var step = number(range.step());
        if (step != null && step.signum() <= 0) {
            invalid(context, range.step(), "MODEL range step must be positive", path + ".step");
            return null;
        }
        var from = number(range.from());
        var to = number(range.to());
        if (from == null || to == null || step == null) return null;
        var distance = range.direction() == RangeDirection.INCREMENT ? to.subtract(from) : from.subtract(to);
        var count = distance.signum() < 0 ? BigDecimal.ZERO : distance.divide(step, 0, RoundingMode.FLOOR).add(BigDecimal.ONE);
        if (count.compareTo(MAX_CELLS) > 0) {
            invalid(context, range, "MODEL FOR range cannot generate more than 10000 values", path);
            return null;
        }
        return count;
    }

    private static void validateWidth(Query query, int width, String path, SchemaValidationContext context) {
        if (query instanceof SelectQuery select
            && select.items().stream().allMatch(item -> item instanceof ExprSelectItem)
            && select.items().size() != width) {
            invalid(context, query, "MODEL FOR query projection width must match dimension count", path);
        }
        if (query instanceof WithQuery with) validateWidth(with.body(), width, path + ".body", context);
        if (query instanceof CompositeQuery composite) {
            for (var index = 0; index < composite.terms().size(); index++) {
                validateWidth(composite.terms().get(index), width, path + ".terms[" + index + "]", context);
            }
        }
    }

    private static BigDecimal number(Expression expression) {
        if (expression instanceof LiteralExpr literal && literal.value() instanceof Number number) {
            try {
                return new BigDecimal(number.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (expression instanceof NegativeArithmeticExpr negative) {
            var value = number(negative.expr());
            return value == null ? null : value.negate();
        }
        if (expression instanceof UnaryOperatorExpr unary && unary.operator().text().equals("+")) return number(unary.expr());
        return null;
    }

    private static void invalid(SchemaValidationContext context, Node node, String message, String path) {
        context.addProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID, message, node, path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<SelectQuery> nodeType() {
        return SelectQuery.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(SelectQuery query, SchemaValidationContext context) {
        if (query.model() == null) return;
        var rules = query.model().main().rules();
        var iteration = rules.iteration();
        if (iteration != null) {
            var limit = number(iteration.limit());
            if (limit == null || limit.signum() <= 0 || limit.stripTrailingZeros().scale() > 0) {
                invalid(context, iteration, "MODEL iteration limit must be a positive integer literal",
                    "model.main.rules.iteration.limit");
            }
            if (rules.order() == RuleOrder.AUTOMATIC) {
                invalid(context, rules, "MODEL AUTOMATIC ORDER cannot be combined with ITERATE", "model.main.rules");
            }
        }
        for (var index = 0; index < rules.rules().size(); index++) {
            var rule = rules.rules().get(index);
            validateTarget(rule.target(), "model.main.rules.rules[" + index + "].target", context);
        }
    }
}
