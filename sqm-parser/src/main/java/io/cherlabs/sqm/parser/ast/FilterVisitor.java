package io.cherlabs.sqm.parser.ast;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.core.views.Columns;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An implementation of a Visitor to handle filter parsing.
 */
public class FilterVisitor implements Expr.Visitor<Filter> {

    private static final Object INVALID = new Object();

    // ---------- helpers ----------
    private static Column toColumn(Expr.Column c) {
        var parts = c.qname();
        String name = parts.get(parts.size() - 1);
        String table = parts.size() > 1 ? parts.get(parts.size() - 2) : null;
        return new NamedColumn(name, null, table);
    }

    private static Column toFunc(Expr.FuncCall f) {
        var name = String.join(".", f.col().qname());
        var args = buildFuncArgs(f);
        return new FunctionColumn(name, args, f.distinct(), null);
    }

    private static Column asColumn(Expr e) {
        if (e instanceof Expr.Column c) {
            return toColumn(c);
        }
        if (e instanceof Expr.FuncCall f) {
            return toFunc(f);
        }
        return null;
    }

    private static Object asScalar(Expr e) {
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

    private static boolean isCmp(String op) {
        return op.equals("=") || op.equals("!=") || op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=");
    }

    private static Filter merge(CompositeFilter.Operator operator, Filter a, Filter b) {
        var list = new ArrayList<Filter>();
        if (a instanceof CompositeFilter ca && ca.op() == operator) {
            list.addAll(ca.filters());
        } else {
            list.add(a);
        }
        if (b instanceof CompositeFilter cb && cb.op() == operator) {
            list.addAll(cb.filters());
        } else {
            list.add(b);
        }
        return new CompositeFilter(operator, list);
    }

    private static Filter fallback(Object node) {
        // simple printer; if you already have one, use it
        return Filter.expr(node.toString());
    }

    private static List<FunctionColumn.Arg> buildFuncArgs(Expr.FuncCall fc) {
        var args = fc.args().stream().map(a -> {
            if (a instanceof Expr.Column col) {
                var nc = toColumn(col);
                return FunctionColumn.Arg.column(Columns.table(nc).orElse(null), Columns.name(nc).orElse(null));
            }
            if (a instanceof Expr.Star) {
                return FunctionColumn.Arg.star();
            }
            if (a instanceof Expr.FuncCall f) {
                var inName = String.join(".", f.col().qname());
                var inArgs = buildFuncArgs(f);
                return FunctionColumn.Arg.func(new FunctionColumn(inName, inArgs, f.distinct(), null));
            }
            return FunctionColumn.Arg.lit(asScalar(a));
        });
        return args.toList();
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
            Column left = asColumn(b.left());
            Column right = asColumn(b.right());
            Object val = asScalar(b.right());

            if (left == null || (val == INVALID && right == null)) return fallback(b);

            Values vs = right == null ? Values.single(val) : Values.column(right);

            return switch (op) {
                case "=" -> new ColumnFilter(left, ColumnFilter.Operator.Eq, vs);
                case "!=" -> new ColumnFilter(left, ColumnFilter.Operator.Ne, vs);
                case "<" -> new ColumnFilter(left, ColumnFilter.Operator.Lt, vs);
                case "<=" -> new ColumnFilter(left, ColumnFilter.Operator.Lte, vs);
                case ">" -> new ColumnFilter(left, ColumnFilter.Operator.Gt, vs);
                case ">=" -> new ColumnFilter(left, ColumnFilter.Operator.Gte, vs);
                case "LIKE" -> new ColumnFilter(left, ColumnFilter.Operator.Like, vs);
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

        // Fallback: unsupported needle (e.g., expr IN (...)) → ExpressionFilter
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
        // NOT BETWEEN -> fallback as expr (or expand to NE of range)
        return fallback(b);
    }

    @Override
    public Filter visitFuncCall(Expr.FuncCall fc) {
        return Filter.column(toFunc(fc));
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
        return Filter.column(Column.val(asScalar(n)));
    }

    @Override
    public Filter visitString(Expr.StringLit s) {
        return Filter.column(Column.val(asScalar(s)));
    }

    @Override
    public Filter visitBool(Expr.BoolLit b) {
        return Filter.column(Column.val(asScalar(b)));
    }

    @Override
    public Filter visitNull(Expr.NullLit n) {
        return Filter.column(Column.val(asScalar(n)));
    }

    @Override
    public Filter visitColumn(Expr.Column c) {
        return Filter.column(toColumn(c));
    }

    @Override
    public Filter visitParam(Expr.Param p) {
        return fallback(p);
    }
}
