package io.cherlabs.sqm.parser.ast;

import java.util.List;

/**
 * A base interface for an expression
 */
public interface Expr {
    <R> R accept(Visitor<R> v);

    /**
     * An interface used in a Visitor pattern
     *
     * @param <R> The type of the entity to be created from the expression.
     */
    interface Visitor<R> {
        R visitBinary(Binary e);

        R visitUnary(Unary e);

        R visitColumn(Column e);

        R visitFuncCall(FuncCall e);

        R visitNumber(NumberLit e);

        R visitString(StringLit e);

        R visitParam(Param e);

        R visitGroup(Group e);

        R visitBool(BoolLit e);

        R visitNull(NullLit e);

        R visitIn(InExpr e);

        R visitLike(LikeExpr e);

        R visitBetween(BetweenExpr e);

        default R visitRow(RowExpr e) {
            return null;
        }
    }

    // Basic nodes

    /**
     * Represents a binary expression.
     *
     * @param left  the left expression.
     * @param op    an operator.
     * @param right the right expression.
     */
    record Binary(Expr left, String op, Expr right) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitBinary(this);
        }

        @Override
        public String toString() {
            return left.toString() + " " + op + " " + right.toString();
        }
    }

    /**
     * Represents a unary expression. For example: {@code NOT TRUE}
     *
     * @param op   a unary operator.
     * @param expr an expression.
     */
    record Unary(String op, Expr expr) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitUnary(this);
        }

        @Override
        public String toString() {
            return op + " " + expr.toString();
        }
    }

    /**
     * Represents a column expression.
     *
     * @param qname a qualified column name such as schema.table.column
     */
    record Column(List<String> qname) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitColumn(this);
        }

        @Override
        public String toString() {
            return String.join(".", qname);
        }
    }

    record FuncCall(Column col, List<Expr> args, boolean distinct) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitFuncCall(this);
        }

        @Override
        public String toString() {
            var dist = distinct ? "distinct" : "";
            return col.toString() + "(" + dist + String.join(", ", args.stream().map(Object::toString).toList()) + ")";
        }
    }

    /**
     * Represents a number.
     *
     * @param text string containing the number.
     */
    record NumberLit(String text) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitNumber(this);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * Represents a string.
     *
     * @param text a text.
     */
    record StringLit(String text) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitString(this);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * Represents a parameter used in a query.
     *
     * @param name the parameter.
     */
    record Param(String name) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitParam(this);
        }

        @Override
        public String toString() {
            return name;
        }
    } // "?" or ":name"

    /**
     * Represents a star '*'.
     */
    record Star() implements Expr {
        @Override
        public <R> R accept(Visitor<R> v) {
            return null;
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    /**
     * Represents an expression inside the parenthesis '(', ')'.
     *
     * @param inner
     */
    record Group(Expr inner) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitGroup(this);
        }

        @Override
        public String toString() {
            return "(" + inner.toString() + ")";
        }
    }

    /**
     * Represents a boolean.
     *
     * @param value a boolean value.
     */
    record BoolLit(boolean value) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitBool(this);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Represents a NULL.
     */
    record NullLit() implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitNull(this);
        }

        @Override
        public String toString() {
            return "NULL";
        }
    }

    // SQL-y extras

    /**
     * Represents an IN expression. For example: {@code col1 IN (1, 2, 3)}
     *
     * @param needle   a left expression. A column or a list of columns in case of a tuple.
     * @param haystack a list of expressions inside the IN.
     * @param negated  indicates whether the expression should be negated or no.
     */
    record InExpr(Expr needle, List<Expr> haystack, boolean negated) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitIn(this);
        }
    }

    // A row-value constructor: (e1, e2, ...), used on LHS or as RHS tuples
    record RowExpr(List<Expr> items) implements Expr {
        @Override
        public <R> R accept(Visitor<R> v) {
            return v.visitRow(this);
        }

        @Override
        public String toString() {
            return "(" + String.join(",", items.stream().map(Object::toString).toList()) + ")";
        }
    }

    /**
     * Represents a LIKE expression.
     *
     * @param left    a left expression.
     * @param pattern a LIKE pattern.
     * @param negated indicates whether the expression should be negated or no.
     */
    record LikeExpr(Expr left, Expr pattern, boolean negated) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitLike(this);
        }
    }

    /**
     * Represents a BETWEEN expression.
     *
     * @param left    a left expression.
     * @param lo      a minimum value.
     * @param hi      a maximum value.
     * @param negated indicates whether the expression should be negated or no.
     */
    record BetweenExpr(Expr left, Expr lo, Expr hi, boolean negated) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitBetween(this);
        }
    }
}
