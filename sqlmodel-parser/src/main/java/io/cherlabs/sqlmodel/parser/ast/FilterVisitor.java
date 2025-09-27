package io.cherlabs.sqlmodel.parser.ast;

import io.cherlabs.sqlmodel.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilterVisitor implements Expr.Visitor<Filter> {

    protected static final Object INVALID = new Object();

    // ---------- helpers ----------
    protected static Column toColumn(Expr.Column c) {
        var parts = c.qname();
        String name = parts.get(parts.size() - 1);
        String table = parts.size() > 1 ? parts.get(parts.size() - 2) : null;
        return new NamedColumn(name, null, table);
    }

    protected static Column asColumn(Expr e) {
        return (e instanceof Expr.Column c) ? toColumn(c) : null;
    }

    protected static Object asScalar(Expr e) {
        if (e instanceof Expr.NumberLit n) {
            String t = n.text();
            if (t.contains(".") || t.contains("e") || t.contains("E")) try {
                return Double.valueOf(t);
            } catch (Exception ignore) {
                return t;
            }
            try {
                return Long.valueOf(t);
            } catch (Exception ignore) {
                return t;
            }
        }
        if (e instanceof Expr.StringLit s) return s.text();
        if (e instanceof Expr.BoolLit b) return b.value();
        if (e instanceof Expr.NullLit) return null;
        if (e instanceof Expr.Param p) return p.name(); // keep as string marker (e.g., "?"," :id")
        return INVALID;
    }

    protected static boolean isCmp(String op) {
        return op.equals("=") || op.equals("!=") || op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=");
    }

    protected static Filter merge(CompositeFilter.Operator operator, Filter a, Filter b) {
        var list = new ArrayList<Filter>();
        if (a instanceof CompositeFilter ca && ca.operator() == operator) {
            list.addAll(ca.filters());
        } else {
            list.add(a);
        }
        if (b instanceof CompositeFilter cb && cb.operator() == operator) {
            list.addAll(cb.filters());
        } else {
            list.add(b);
        }
        return new CompositeFilter(operator, list);
    }

    protected static Filter fallback(Object node) {
        // simple printer; if you already have one, use it
        return Filter.expr(node.toString());
    }

    // ---------- Boolean composition ----------
    @Override
    public Filter visitBinary(Expr.Binary b) {
        String op = b.op().toUpperCase(Locale.ROOT);

        if (op.equals("AND") || op.equals("OR")) {
            Filter L = b.left().accept(this);
            Filter R = b.right().accept(this);
            return merge(op.equals("AND") ? CompositeFilter.Operator.And : CompositeFilter.Operator.Or, L, R);
        }

        // IS / IS NOT NULL normalized by parser
        if (op.equals("IS") || op.equals("IS NOT")) {
            Column col = asColumn(b.left());
            if (col == null || !(b.right() instanceof Expr.NullLit)) return fallback(b);
            return new ColumnFilter(col, op.equals("IS NOT") ? ColumnFilter.Operator.IsNotNull : ColumnFilter.Operator.IsNull, null);
        }

        // comparisons & LIKE between col and value
        if (isCmp(op) || op.equals("LIKE")) {
            // col <> lit/param  --> ColumnFilter
            Column col = asColumn(b.left());
            Object val = asScalar(b.right());
            if (col == null || val == INVALID) return fallback(b);

            return switch (op) {
                case "=" -> new ColumnFilter(col, ColumnFilter.Operator.Eq, Values.single(val));
                case "!=" -> new ColumnFilter(col, ColumnFilter.Operator.Ne, Values.single(val));
                case "<" -> new ColumnFilter(col, ColumnFilter.Operator.Lt, Values.single(val));
                case "<=" -> new ColumnFilter(col, ColumnFilter.Operator.Lte, Values.single(val));
                case ">" -> new ColumnFilter(col, ColumnFilter.Operator.Gt, Values.single(val));
                case ">=" -> new ColumnFilter(col, ColumnFilter.Operator.Gte, Values.single(val));
                case "LIKE" -> new ColumnFilter(col, ColumnFilter.Operator.Like, Values.single(val));
                default -> fallback(b);
            };
        }

        return fallback(b);
    }

    @Override
    public Filter visitLike(Expr.LikeExpr l) {
        Column col = asColumn(l.left());
        Object pat = asScalar(l.pattern());
        if (col == null || pat == INVALID) return fallback(l);
        return new ColumnFilter(col, l.negated() ? ColumnFilter.Operator.NotLike : ColumnFilter.Operator.Like, Values.single(pat));
    }

    @Override
    public Filter visitIn(Expr.InExpr in) {
        // Tuple IN: (col1,col2,...) IN ( (..), (..), ... )
        if (in.needle() instanceof Expr.RowExpr lhs) {
            List<Column> cols = new ArrayList<>(lhs.items().size());
            for (Expr item : lhs.items()) {
                if (!(item instanceof Expr.Column c)) return fallback(in); // not strictly a tuple of columns
                cols.add(toColumn(c));
            }
            List<List<Object>> rows = new ArrayList<>(in.haystack().size());
            for (Expr row : in.haystack()) {
                if (!(row instanceof Expr.RowExpr rr)) return fallback(in); // RHS must be tuples
                List<Object> vals = new ArrayList<>(rr.items().size());
                for (Expr v : rr.items()) {
                    Object lit = asScalar(v);
                    if (lit == INVALID) return fallback(in);
                    vals.add(lit);
                }
                if (vals.size() != cols.size()) return fallback(in); // arity mismatch
                rows.add(List.copyOf(vals));
            }
            var values = Values.tuples(rows);
            return new TupleFilter(cols, in.negated() ? TupleFilter.Operator.NotIn : TupleFilter.Operator.In, values);
        }

        // Simple IN: col IN (v1, v2, ...)
        if (in.needle() instanceof Expr.Column c) {
            Column col = toColumn(c);
            List<Object> vals = new ArrayList<>(in.haystack().size());
            for (Expr v : in.haystack()) {
                Object lit = asScalar(v);
                if (lit == INVALID) return fallback(in);
                vals.add(lit);
            }
            var values = Values.list(vals);
            return new ColumnFilter(col, in.negated() ? ColumnFilter.Operator.NotIn : ColumnFilter.Operator.In, values);
        }

        // Fallback: unsupported needle (e.g., expression IN (...)) → ExpressionFilter
        return fallback(in);
    }

    @Override
    public Filter visitBetween(Expr.BetweenExpr b) {
        Column col = asColumn(b.left());
        Object lo = asScalar(b.lo());
        Object hi = asScalar(b.hi());
        if (col == null || lo == INVALID || hi == INVALID) return fallback(b);
        if (!b.negated()) {
            return new ColumnFilter(col, ColumnFilter.Operator.Range, Values.range(lo, hi));
        }
        // NOT BETWEEN -> fallback as expression (or expand to NE of range)
        return fallback(b);
    }

    // ---------- Leaf-ish ----------
    @Override
    public Filter visitGroup(Expr.Group g) {
        return g.inner().accept(this);
    }

    @Override
    public Filter visitUnary(Expr.Unary u) {
        return Filter.not((u.expr().accept(this)));
    } // If you want, implement NOT-pushing

    @Override
    public Filter visitNumber(Expr.NumberLit n) {
        return fallback(n);
    }

    @Override
    public Filter visitString(Expr.StringLit s) {
        return fallback(s);
    }

    @Override
    public Filter visitBool(Expr.BoolLit b) {
        return fallback(b);
    }

    @Override
    public Filter visitNull(Expr.NullLit n) {
        return fallback(n);
    }

    @Override
    public Filter visitColumn(Expr.Column c) {
        return fallback(c);
    }

    @Override
    public Filter visitParam(Expr.Param p) {
        return fallback(p);
    }
}
