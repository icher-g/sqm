package io.sqm.core.transform;

import io.sqm.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Normalizes identifier values in the SQL model for deterministic rendering/fingerprinting.
 *
 * <p>Only unquoted identifiers ({@link QuoteStyle#NONE}) are normalized. Quoted identifiers are preserved
 * exactly because their lexical form may be semantically significant.</p>
 *
 * <p>The current normalization strategy lower-cases unquoted identifier values using {@link Locale#ROOT}.</p>
 */
public final class IdentifierNormalizationTransformer extends RecursiveNodeTransformer {
    private final IdentifierNormalizationCaseMode caseMode;

    /**
     * Creates an identifier normalization transformer.
     */
    public IdentifierNormalizationTransformer() {
        this(IdentifierNormalizationCaseMode.LOWER);
    }

    /**
     * Creates an identifier normalization transformer with explicit case mode for unquoted identifiers.
     *
     * @param caseMode case normalization mode for unquoted identifiers
     */
    public IdentifierNormalizationTransformer(IdentifierNormalizationCaseMode caseMode) {
        this.caseMode = Objects.requireNonNull(caseMode, "caseMode");
    }

    @Override
    public Node visitTable(Table t) {
        var schema = normalizeIdentifier(t.schema());
        var name = normalizeIdentifier(t.name());
        var alias = normalizeIdentifier(t.alias());
        if (schema == t.schema() && name == t.name() && alias == t.alias()) {
            return t;
        }
        return Table.of(schema, name, alias, t.inheritance());
    }

    @Override
    public Node visitColumnExpr(ColumnExpr c) {
        var tableAlias = normalizeIdentifier(c.tableAlias());
        var name = normalizeIdentifier(c.name());
        if (tableAlias == c.tableAlias() && name == c.name()) {
            return c;
        }
        return ColumnExpr.of(tableAlias, name);
    }

    @Override
    public Node visitExprSelectItem(ExprSelectItem i) {
        var expr = apply(i.expr());
        var alias = normalizeIdentifier(i.alias());
        if (expr == i.expr() && alias == i.alias()) {
            return i;
        }
        return ExprSelectItem.of(expr, alias);
    }

    @Override
    public Node visitQualifiedStarSelectItem(QualifiedStarSelectItem i) {
        var qualifier = normalizeIdentifier(i.qualifier());
        if (qualifier == i.qualifier()) {
            return i;
        }
        return QualifiedStarSelectItem.of(qualifier);
    }

    @Override
    public Node visitQueryTable(QueryTable t) {
        var query = apply(t.query());
        var alias = normalizeIdentifier(t.alias());
        var columnAliases = normalizeIdentifiers(t.columnAliases());
        if (query == t.query() && alias == t.alias() && columnAliases == t.columnAliases()) {
            return t;
        }
        return QueryTable.of(query, alias, columnAliases);
    }

    @Override
    public Node visitValuesTable(ValuesTable t) {
        var rows = apply(t.values());
        var alias = normalizeIdentifier(t.alias());
        var columnAliases = normalizeIdentifiers(t.columnAliases());
        if (rows == t.values() && alias == t.alias() && columnAliases == t.columnAliases()) {
            return t;
        }
        return ValuesTable.of(rows, alias, columnAliases);
    }

    @Override
    public Node visitFunctionTable(FunctionTable t) {
        var function = apply(t.function());
        var alias = normalizeIdentifier(t.alias());
        var columnAliases = normalizeIdentifiers(t.columnAliases());
        if (function == t.function() && alias == t.alias() && columnAliases == t.columnAliases()) {
            return t;
        }
        return FunctionTable.of(function, columnAliases, alias, t.ordinality());
    }

    @Override
    public Node visitUsingJoin(UsingJoin j) {
        var right = apply(j.right());
        var usingColumns = normalizeIdentifiers(j.usingColumns());
        if (right == j.right() && usingColumns == j.usingColumns()) {
            return j;
        }
        return UsingJoin.of(right, j.kind(), usingColumns);
    }

    @Override
    public Node visitFunctionExpr(FunctionExpr f) {
        var name = normalizeQualifiedName(f.name());
        List<FunctionExpr.Arg> args = new ArrayList<>();
        boolean changed = apply(f.args(), args);
        var withinGroup = apply(f.withinGroup());
        changed |= withinGroup != f.withinGroup();
        var filter = apply(f.filter());
        changed |= filter != f.filter();
        var over = apply(f.over());
        changed |= over != f.over();
        changed |= name != f.name();
        if (!changed) {
            return f;
        }
        return FunctionExpr.of(name, args, f.distinctArg(), withinGroup, filter, over);
    }

    @Override
    public Node visitCte(CteDef c) {
        var name = normalizeIdentifier(c.name());
        var body = apply(c.body());
        var columnAliases = normalizeIdentifiers(c.columnAliases());
        if (name == c.name() && body == c.body() && columnAliases == c.columnAliases()) {
            return c;
        }
        return CteDef.of(name, body, columnAliases, c.materialization());
    }

    @Override
    public Node visitWindowDef(WindowDef w) {
        var name = normalizeIdentifier(w.name());
        var spec = apply(w.spec());
        if (name == w.name() && spec == w.spec()) {
            return w;
        }
        return WindowDef.of(name, spec);
    }

    @Override
    public Node visitOverRef(OverSpec.Ref r) {
        var name = normalizeIdentifier(r.windowName());
        if (name == r.windowName()) {
            return r;
        }
        return OverSpec.ref(name);
    }

    @Override
    public Node visitOverDef(OverSpec.Def d) {
        var baseWindow = normalizeIdentifier(d.baseWindow());
        var partitionBy = apply(d.partitionBy());
        var orderBy = apply(d.orderBy());
        var frame = apply(d.frame());
        if (baseWindow == d.baseWindow() && partitionBy == d.partitionBy() && orderBy == d.orderBy() && frame == d.frame()) {
            return d;
        }
        if (baseWindow == null) {
            return OverSpec.def(partitionBy, orderBy, frame, d.exclude());
        }
        return OverSpec.def(baseWindow, orderBy, frame, d.exclude());
    }

    @Override
    public Node visitOrderItem(OrderItem i) {
        var expr = apply(i.expr());
        var collate = normalizeQualifiedName(i.collate());
        if (expr == i.expr() && collate == i.collate()) {
            return i;
        }
        return OrderItem.of(expr, i.ordinal(), i.direction(), i.nulls(), collate, i.usingOperator());
    }

    @Override
    public Node visitCollateExpr(CollateExpr expr) {
        var operand = apply(expr.expr());
        var collation = normalizeQualifiedName(expr.collation());
        if (operand == expr.expr() && collation == expr.collation()) {
            return expr;
        }
        return CollateExpr.of(operand, collation);
    }

    @Override
    public Node visitCastExpr(CastExpr expr) {
        var operand = apply(expr.expr());
        var type = (TypeName) visitTypeName(expr.type());
        if (operand == expr.expr() && type == expr.type()) {
            return expr;
        }
        return CastExpr.of(operand, type);
    }

    @Override
    public Node visitBinaryOperatorExpr(BinaryOperatorExpr expr) {
        var left = apply(expr.left());
        var right = apply(expr.right());
        var operator = normalizeOperatorName(expr.operator());
        if (left == expr.left() && right == expr.right() && operator == expr.operator()) {
            return expr;
        }
        return BinaryOperatorExpr.of(left, operator, right);
    }

    @Override
    public Node visitUnaryOperatorExpr(UnaryOperatorExpr expr) {
        var operand = apply(expr.expr());
        var operator = normalizeOperatorName(expr.operator());
        if (operand == expr.expr() && operator == expr.operator()) {
            return expr;
        }
        return UnaryOperatorExpr.of(operator, operand);
    }

    @Override
    public Node visitTypeName(TypeName typeName) {
        var qualifiedName = normalizeQualifiedName(typeName.qualifiedName());
        List<Expression> modifiers = new ArrayList<>();
        boolean changed = apply(typeName.modifiers(), modifiers);
        changed |= qualifiedName != typeName.qualifiedName();
        if (!changed) {
            return typeName;
        }
        return TypeName.of(qualifiedName, typeName.keyword().orElse(null), modifiers, typeName.arrayDims(), typeName.timeZoneSpec());
    }

    @Override
    public Node visitLockingClause(LockingClause clause) {
        if (clause.ofTables().isEmpty()) {
            return clause;
        }
        boolean changed = false;
        var targets = new ArrayList<LockTarget>(clause.ofTables().size());
        for (var target : clause.ofTables()) {
            var normalized = normalizeIdentifier(target.identifier());
            if (normalized != target.identifier()) {
                changed = true;
                targets.add(LockTarget.of(normalized));
            } else {
                targets.add(target);
            }
        }
        if (!changed) {
            return clause;
        }
        return LockingClause.of(clause.mode(), targets, clause.nowait(), clause.skipLocked());
    }

    private Identifier normalizeIdentifier(Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        if (identifier.quoteStyle() != QuoteStyle.NONE) {
            return identifier;
        }
        String normalized = switch (caseMode) {
            case LOWER -> identifier.value().toLowerCase(Locale.ROOT);
            case UPPER -> identifier.value().toUpperCase(Locale.ROOT);
            case UNCHANGED -> identifier.value();
        };
        if (normalized.equals(identifier.value())) {
            return identifier;
        }
        return Identifier.of(normalized, identifier.quoteStyle());
    }

    private List<Identifier> normalizeIdentifiers(List<Identifier> identifiers) {
        Objects.requireNonNull(identifiers, "identifiers");
        List<Identifier> normalized = null;
        for (int i = 0; i < identifiers.size(); i++) {
            var current = identifiers.get(i);
            var next = normalizeIdentifier(current);
            if (normalized == null && next != current) {
                normalized = new ArrayList<>(identifiers);
            }
            if (normalized != null) {
                normalized.set(i, next);
            }
        }
        return normalized == null ? identifiers : List.copyOf(normalized);
    }

    private QualifiedName normalizeQualifiedName(QualifiedName qualifiedName) {
        if (qualifiedName == null) {
            return null;
        }
        var normalizedParts = normalizeIdentifiers(qualifiedName.parts());
        if (normalizedParts == qualifiedName.parts()) {
            return qualifiedName;
        }
        return new QualifiedName(normalizedParts);
    }

    private OperatorName normalizeOperatorName(OperatorName operatorName) {
        var schemaName = normalizeQualifiedName(operatorName.schemaName());
        if (schemaName == operatorName.schemaName()) {
            return operatorName;
        }
        return new OperatorName(schemaName, operatorName.symbol(), operatorName.syntax());
    }
}
