package io.cherlabs.sqlmodel.parser.ast;

import java.util.*;

public interface Expr {
    <R> R accept(Visitor<R> v);

    interface Visitor<R> {
        R visitBinary(Binary e);
        R visitUnary(Unary e);
        R visitColumn(Column e);
        R visitNumber(NumberLit e);
        R visitString(StringLit e);
        R visitParam(Param e);
        R visitGroup(Group e);
        R visitBool(BoolLit e);
        R visitNull(NullLit e);
        R visitIn(InExpr e);
        R visitLike(LikeExpr e);
        R visitBetween(BetweenExpr e);

        default R visitRow(RowExpr e) { return null; }
    }

    // Basic nodes
    record Binary(Expr left, String op, Expr right) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitBinary(this);
        }
    }

    record Unary(String op, Expr expr) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitUnary(this);
        }
    }

    record Column(List<String> qname) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitColumn(this);
        }
    }

    record NumberLit(String text) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitNumber(this);
        }
    }

    record StringLit(String text) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitString(this);
        }
    }

    record Param(String name) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitParam(this);
        }
    } // "?" or ":name"

    record Group(Expr inner) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitGroup(this);
        }
    }

    record BoolLit(boolean value) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitBool(this);
        }
    }

    record NullLit() implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitNull(this);
        }
    }

    // SQL-y extras
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

    record LikeExpr(Expr left, Expr pattern, boolean negated) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitLike(this);
        }
    }

    record BetweenExpr(Expr left, Expr lo, Expr hi, boolean negated) implements Expr {
        public <R> R accept(Visitor<R> v) {
            return v.visitBetween(this);
        }
    }
}
