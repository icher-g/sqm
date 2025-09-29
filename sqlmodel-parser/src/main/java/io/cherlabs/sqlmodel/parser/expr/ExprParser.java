package io.cherlabs.sqlmodel.parser.expr;

import io.cherlabs.sqlmodel.parser.ast.Expr;
import io.cherlabs.sqlmodel.parser.core.*;

import java.util.ArrayList;
import java.util.List;

import static io.cherlabs.sqlmodel.parser.core.TokenType.*;

public final class ExprParser {
    private final Cursor cur;

    public ExprParser(String input) {
        this(Lexer.lexAll(input));
    }

    public ExprParser(List<Token> tokens) {
        this(new Cursor(tokens));
    }

    public ExprParser(Cursor cur) {
        this.cur = cur;
    }

    public Expr parseExpr() {
        Expr e = parseOr();
        expect(EOF, "Unexpected trailing tokens");
        return e;
    }

    // orExpr := andExpr ( OR andExpr )*
    private Expr parseOr() {
        Expr left = parseAnd();
        while (match(OR)) left = new Expr.Binary(left, "OR", parseAnd());
        return left;
    }

    // andExpr := notExpr ( AND notExpr )*
    private Expr parseAnd() {
        Expr left = parseNot();
        while (match(AND)) left = new Expr.Binary(left, "AND", parseNot());
        return left;
    }

    // notExpr := [NOT]* cmpExpr
    private Expr parseNot() {
        if (match(NOT)) return new Expr.Unary("NOT", parseNot());
        return parseCmp();
    }

    /**
     * cmpExpr handles:
     * additive ( (=,!=,<,<=,>,>=) additive )?
     * additive [NOT] IN '(' expr (',' expr)* ')'
     * additive [NOT] LIKE additive
     * additive [NOT] BETWEEN additive AND additive
     * additive IS [NOT] NULL
     */
    private Expr parseCmp() {
        Expr left = parseAdd();

        // IS [NOT] NULL
        if (match(IS)) {
            boolean neg = match(NOT);
            expect(NULL, "Expected NULL after IS/IS NOT");
            return new Expr.Binary(left, neg ? "IS NOT" : "IS", new Expr.NullLit());
        }

        // [NOT] BETWEEN
        if (peekIs(BETWEEN) || (peekIs(NOT) && peekIs(BETWEEN, 1))) {
            boolean neg = match(NOT); // optional
            expect(BETWEEN, "Expected BETWEEN");
            Expr lo = parseAdd();
            expect(AND, "Expected AND in BETWEEN");
            Expr hi = parseAdd();
            return new Expr.BetweenExpr(left, lo, hi, neg);
        }

        // [NOT] IN (...)
        if (peekIs(IN) || (peekIs(NOT) && peekIs(IN, 1))) {
            boolean neg = match(NOT);
            expect(IN, "Expected IN");
            expect(LPAREN, "Expected '(' after IN");
            // Tuple-IN: next token is '('  → parse list of RowExpr: ( (..), (..), ... )
            if (peekIs(LPAREN)) {
                List<Expr> rows = new ArrayList<>();
                do {
                    rows.add(parseRowExpr());      // consumes , ( ... )
                }
                while (match(COMMA));
                expect(RPAREN, "Expected ')' to close IN tuple list");
                return new Expr.InExpr(left, rows, neg);
            }

            // Simple IN list: ( expr, expr, ... )
            List<Expr> items = new ArrayList<>();
            if (!peekIs(RPAREN)) {
                do items.add(parseAdd());
                while (match(COMMA));
            }
            expect(RPAREN, "Expected ')' to close IN list");
            return new Expr.InExpr(left, items, neg);
        }

        // [NOT] LIKE rhs
        if (peekIs(LIKE) || (peekIs(NOT) && peekIs(LIKE, 1))) {
            boolean neg = match(NOT);
            expect(LIKE, "Expected LIKE");
            Expr pat = parseAdd();
            return new Expr.LikeExpr(left, pat, neg);
        }

        // Simple comparisons
        if (match(EQ) || match(NEQ1) || match(NEQ2) || match(LT) || match(LTE) || match(GT) || match(GTE)) {
            Token op = prev();
            String opText = switch (op.type()) {
                case EQ -> "=";
                case NEQ1, NEQ2 -> "!=";
                case LT -> "<";
                case LTE -> "<=";
                case GT -> ">";
                case GTE -> ">=";
                default -> op.lexeme();
            };
            Expr right = parseAdd();
            return new Expr.Binary(left, opText, right);
        }

        return left;
    }

    /* ---------- row parser used by IN tuple RHS ---------- */

    private Expr.RowExpr parseRowExpr() {
        expect(LPAREN, "Expected '(' to start tuple");
        List<Expr> items = new ArrayList<>();
        items.add(parseOr());
        expect(COMMA, "Tuple must contain at least two items");
        do items.add(parseOr());
        while (match(COMMA));
        expect(RPAREN, "Expected ')' to close tuple");
        return new Expr.RowExpr(List.copyOf(items));
    }

    // add/sub, mul/div, power, unary, primary

    private Expr parseAdd() {
        Expr left = parseMul();
        while (match(PLUS) || match(MINUS)) {
            Token op = prev();
            left = new Expr.Binary(left, op.lexeme(), parseMul());
        }
        return left;
    }

    private Expr parseMul() {
        Expr left = parsePow();
        while (match(STAR) || match(SLASH)) {
            Token op = prev();
            left = new Expr.Binary(left, op.lexeme(), parsePow());
        }
        return left;
    }

    private Expr parsePow() {
        Expr left = parseUnary();
        if (match(CARET)) {
            // right-assoc
            left = new Expr.Binary(left, "^", parsePow());
        }
        return left;
    }

    private Expr parseUnary() {
        if (match(MINUS)) return new Expr.Unary("-", parseUnary());
        return parsePrimary();
    }

    private Expr parsePrimary() {
        Token t = advance();
        return switch (t.type()) {
            case LPAREN -> {
                // Try to see if this is a row (has a comma before the closing ')')
                List<Expr> items = new ArrayList<>();
                items.add(parseOr()); // first item inside '('
                if (match(COMMA)) {
                    // It's a row: parse the rest until ')'
                    do items.add(parseOr());
                    while (match(COMMA));
                    expect(RPAREN, "Expected ')' to close row");
                    yield new Expr.RowExpr(List.copyOf(items));
                }
                else {
                    // Not a row → must be grouping "( expr )"
                    expect(RPAREN, "Expected ')' to close group");
                    yield new Expr.Group(items.get(0));
                }
            }
            case IDENT -> {
                var parts = new ArrayList<String>();
                parts.add(t.lexeme());
                while (match(DOT)) parts.add(expect(IDENT, "Expected identifier after '.'").lexeme());
                yield new Expr.Column(parts);
            }
            case NUMBER -> new Expr.NumberLit(t.lexeme());
            case STRING -> new Expr.StringLit(t.lexeme());
            case PARAM_QMARK, PARAM_NAMED -> new Expr.Param(t.lexeme());
            case TRUE -> new Expr.BoolLit(true);
            case FALSE -> new Expr.BoolLit(false);
            case NULL -> new Expr.NullLit();
            default -> throw new ParserException("Expected expr term", t.pos());
        };
    }

    // --- helpers ---

    private Token prev() {
        return cur.prev();
    }

    private boolean peekIs(TokenType tt) {
        return cur.match(tt);
    }

    private boolean peekIs(TokenType tt, int lookahead) {
        return cur.match(tt, lookahead);
    }

    private Token advance() {
        return cur.advance();
    }

    private boolean match(TokenType tt) {
        return cur.consumeIf(tt);
    }

    private Token expect(TokenType tt, String msg) {
        return cur.expect(msg, tt);
    }
}
