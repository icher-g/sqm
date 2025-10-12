package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.ParserException;
import io.cherlabs.sqm.parser.core.TokenType;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.copyOf;

/**
 * A spec parser for column specifications.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     "u.user_name", "o.status", "count(*) AS cnt"
 *     }
 * </pre>
 */
public class ColumnParser implements Parser<Column> {

    private static Object parseNumber(String lexeme) {
        // match FilterSpecParser number policy: prefer Long, fallback Double
        try {
            if (lexeme.contains(".") || lexeme.contains("e") || lexeme.contains("E")) {
                return Double.valueOf(lexeme);
            }
            return Long.valueOf(lexeme);
        } catch (NumberFormatException nfe) {
            // fallback to string if something exotic slips through
            return lexeme;
        }
    }

    private static String unescapeSqlString(String tokenLexeme) {
        // token lexeme is usually without surrounding quotes if Lexer strips them; if not, trim them first:
        String s = tokenLexeme;
        if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            s = s.substring(1, s.length() - 1);
        }
        // SQL escape of single quote is doubled: ''
        return s.replace("''", "'");
    }

    /**
     * Gets the {@link Column} type.
     *
     * @return {@link Column} type.
     */
    @Override
    public Class<Column> targetType() {
        return Column.class;
    }

    /**
     * Parses the column specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
    @Override
    public ParseResult<Column> parse(Cursor cur) {
        try {
            // The column spec might be inside the brackets ().
            cur.consumeIf(TokenType.LPAREN);

            // Check if this is a *.
            if (cur.consumeIf(TokenType.STAR)) {
                return finalize(cur, ParseResult.ok(new StarColumn()), null);
            }

            // Try value: SELECT 1
            if (looksLikeValue(cur)) {
                var vr = parseValueColumn(cur);
                return finalize(cur, vr, "Unexpected tokens after value.");
            }

            // Try CASE
            if (looksLikeCase(cur)) {
                var cr = parseCaseColumn(cur);
                return finalize(cur, cr, "Unexpected tokens after CASE...END.");
            }

            // Try function column: IDENT ('.' IDENT)* '(' ...
            if (looksLikeFunctionAt(cur)) {
                var fr = parseFunctionColumn(cur);
                return finalize(cur, fr, "Unexpected tokens after function column.");
            }

            // Simple expr like: t.c [AS a] | c [AS a]
            var nr = parseNamedColumn(cur);
            return finalize(cur, nr, "Unexpected tokens after column alias.");
        } catch (ParserException ex) {
            return ParseResult.error(ex.getMessage());
        }
    }

    private ParseResult<Column> finalize(Cursor cur, ParseResult<Column> pr, String error) {
        if (!pr.ok()) return ParseResult.error(pr);
        cur.consumeIf(TokenType.RPAREN); // in case it was in brackets.
        if (!cur.isEof()) {
            return ParseResult.error(error, cur.pos());
        }
        return pr;
    }

    private ParseResult<Column> parseValueColumn(Cursor cur) {
        var t = cur.advance();
        Object value = t.lexeme();
        if (t.type() == TokenType.NUMBER) {
            value = parseNumber(t.lexeme());
        }
        var alias = tryParseAlias(cur);
        return ParseResult.ok(new ValueColumn(value, alias));
    }

    private ParseResult<Column> parseFunctionColumn(Cursor cur) {
        // parse the call starting at 0
        var funcResult = parseFunctionCall(cur);
        if (!funcResult.ok()) return ParseResult.error(funcResult);

        // optional alias: AS identifier | bare identifier
        String alias = null;
        if (cur.consumeIf(TokenType.AS)) {
            if (!cur.match(TokenType.IDENT)) return ParseResult.error("Expected alias after AS", cur.pos());
            alias = cur.advance().lexeme();
        } else if (cur.match(TokenType.IDENT)) {
            alias = cur.advance().lexeme();
        }
        return ParseResult.ok(funcResult.value().as(alias));
    }

    private ParseResult<Column> parseNamedColumn(Cursor cur) {
        // 1) Try: table.name or name
        if (cur.match(TokenType.IDENT)) {
            String table = null, name = null, alias;

            // Option A: t1.c1 [alias]
            if (cur.peek(1).type() == TokenType.DOT && cur.peek(2).type() == TokenType.IDENT) {
                table = cur.advance().lexeme();
                cur.advance(); // skip DOT
                name = cur.advance().lexeme();
            } else {
                // Option B: c1 [alias]
                if (cur.matchAny(1, TokenType.AS, TokenType.IDENT, TokenType.EOF)) {
                    name = cur.advance().lexeme();
                }
            }

            alias = tryParseAlias(cur);
            return ParseResult.ok(new NamedColumn(name, alias, table));
        }

        return ParseResult.error("Unexpected tokens at the beginning of expr, expected identifier but found: " + cur.peek(), cur.pos());
    }

    private ParseResult<Column> parseCaseColumn(Cursor cur) {
        // CASE
        cur.expect("Expected CASE but found: " + cur.peek(), TokenType.CASE);

        // One or more WHEN ... THEN ...
        var whens = new ArrayList<WhenThen>();
        while (cur.consumeIf(TokenType.WHEN)) {
            var end = cur.find(TokenType.THEN);
            if (end == cur.size()) {
                return ParseResult.error("Expected THEN after WHEN <predicate>", cur.pos());
            }

            var fr = new FilterParser().parse(cur.advance(end));
            if (!fr.ok()) {
                return ParseResult.error(fr);
            }

            // THEN
            cur.expect("Expected THEN after WHEN <predicate>", TokenType.THEN);

            // <result>
            var thenResult = parseResultExpr(cur);
            if (!thenResult.ok()) {
                return ParseResult.error(thenResult);
            }

            whens.add(new WhenThen(fr.value(), thenResult.value()));
        }

        if (whens.isEmpty()) {
            return ParseResult.error("CASE must have at least one WHEN ... THEN arm", cur.pos());
        }

        // Optional ELSE <result>
        Entity elseValue = null;

        if (cur.consumeIf(TokenType.ELSE)) {
            var elseResult = parseResultExpr(cur);
            if (!elseResult.ok()) {
                return ParseResult.error(elseResult);
            }
            elseValue = elseResult.value();
        }

        // END
        cur.expect("Expected END to close CASE", TokenType.END);

        // Optional AS alias
        String alias = tryParseAlias(cur);
        return ParseResult.ok(new CaseColumn(whens, elseValue, alias));
    }

    /**
     * Parses a CASE result expr:
     * - string/number/boolean/null literal → boxed Java Object
     * - qualified identifier like  t.col  or  "T"."Name"  → Column.of(...).from(...)
     * - parenthesized CASE (nested) → delegate to this parser (optional; shown here)
     * <p>
     * If you already have a ColumnSpecParser / ValueSpecParser, swap them in here.
     */
    private ParseResult<Entity> parseResultExpr(Cursor cur) {
        // Literals
        if (cur.match(TokenType.STRING)) {
            return ParseResult.ok(Values.single(cur.advance().lexeme()));
        }
        if (cur.match(TokenType.NUMBER)) {
            return ParseResult.ok(Values.single(parseNumber(cur.advance().lexeme())));
        }
        if (cur.match(TokenType.NULL)) {
            cur.advance(); // skip the literal itself
            return ParseResult.ok(Values.single(null));
        }
        if (cur.match(TokenType.TRUE)) {
            cur.advance(); // skip the literal itself
            return ParseResult.ok(Values.single(Boolean.TRUE));
        }
        if (cur.match(TokenType.FALSE)) {
            cur.advance(); // skip the literal itself
            return ParseResult.ok(Values.single(Boolean.FALSE));
        }

        // Nested CASE
        if (cur.match(TokenType.CASE)) {
            var pr = parseCaseColumn(cur);
            if (!pr.ok()) return ParseResult.error(pr);
            return ParseResult.ok(pr.value());
        }

        // Otherwise: treat as a column reference (possibly qualified)
        var columnResult = parseNamedColumn(cur);
        if (columnResult.ok()) return ParseResult.ok(columnResult.value());

        return ParseResult.error("Expected literal, column, or nested CASE", cur.pos());
    }

    /**
     * Try to parse alias: either `AS IDENT` or bare `IDENT`. Returns (alias, nextIndex).
     */
    private String tryParseAlias(Cursor cur) {
        if (cur.consumeIf(TokenType.AS)) {
            if (cur.match(TokenType.IDENT)) {
                return cur.advance().lexeme();
            } else {
                throw new ParserException("Expected alias after AS", cur.pos());
            }
        }
        if (cur.match(TokenType.IDENT)) {
            return cur.advance().lexeme();
        }
        // no alias
        return null;
    }

    /**
     * Returns true iff at idx we have IDENT ('.' IDENT)* '(' — i.e., a function call begins.
     */
    private boolean looksLikeFunctionAt(Cursor cur) {
        if (!cur.match(TokenType.IDENT)) return false;
        int p = 1;
        while (cur.match(TokenType.DOT, p) && cur.match(TokenType.IDENT, p + 1)) {
            p += 2;
        }
        return cur.match(TokenType.LPAREN, p);
    }

    private boolean looksLikeCase(Cursor cur) {
        return cur.match(TokenType.CASE);
    }

    private boolean looksLikeValue(Cursor cur) {
        return cur.matchAny(TokenType.NUMBER, TokenType.STRING);
    }

    /**
     * Parse ONLY a function call (no alias), starting at idx. Returns {FunctionColumn, nextIndex}.
     */
    private ParseResult<FunctionColumn> parseFunctionCall(Cursor cur) {
        // name: IDENT ('.' IDENT)*
        var t = cur.expect("Expected function name", TokenType.IDENT);

        StringBuilder name = new StringBuilder(t.lexeme());
        while (cur.consumeIf(TokenType.DOT) && cur.match(TokenType.IDENT, 1)) {
            name.append('.').append(cur.advance().lexeme());
        }

        // '('
        cur.expect("Expected '(' after function name", TokenType.LPAREN);

        boolean distinct = cur.consumeIf(TokenType.DISTINCT);

        List<FunctionColumn.Arg> args = new ArrayList<>();
        if (cur.consumeIf(TokenType.STAR)) { // '*'
            args.add(new FunctionColumn.Arg.Star());
        } else if (!cur.match(TokenType.RPAREN)) {
            // one or more comma-separated args
            while (true) {
                var argResult = parseFunctionArg(cur); // parses one arg (may include nested function)
                if (!argResult.ok()) {
                    return ParseResult.error(argResult);
                }
                args.add(argResult.value());
                if (cur.consumeIf(TokenType.COMMA)) {
                    continue;
                }
                break;
            }
        }

        // ')'
        cur.expect("Expected ')' to close function", TokenType.RPAREN);

        return ParseResult.ok(new FunctionColumn(name.toString(), copyOf(args), distinct, null));
    }

    /**
     * Parse one function argument (column ref, literal, nested function, or '*').
     */
    private ParseResult<FunctionColumn.Arg> parseFunctionArg(Cursor cur) {
        // Nested function: IDENT ('.' IDENT)* '(' ...
        if (looksLikeFunctionAt(cur)) {
            var funcResult = parseFunctionCall(cur);
            if (!funcResult.ok()) return ParseResult.error(funcResult); // <— no alias, no EOF check here
            return ParseResult.ok(new FunctionColumn.Arg.Function(funcResult.value()));
        }

        // ColumnRef: IDENT ('.' IDENT)?
        if (cur.match(TokenType.IDENT)) {
            String first = cur.advance().lexeme();
            if (cur.consumeIf(TokenType.DOT)) {
                var t = cur.expect("Expected identifier after '.'", TokenType.IDENT);
                String second = t.lexeme();
                return ParseResult.ok(new FunctionColumn.Arg.Column(first, second));
            } else {
                return ParseResult.ok(new FunctionColumn.Arg.Column(null, first));
            }
        }

        // Literals
        switch (cur.peek().type()) {
            case NUMBER -> {
                Object num = parseNumber(cur.advance().lexeme());
                return ParseResult.ok(new FunctionColumn.Arg.Literal(num));
            }
            case STRING -> {
                String s = unescapeSqlString(cur.advance().lexeme());
                return ParseResult.ok(new FunctionColumn.Arg.Literal(s));
            }
            case TRUE -> {
                cur.advance(); // skip the literal itself
                return ParseResult.ok(new FunctionColumn.Arg.Literal(Boolean.TRUE));
            }
            case FALSE -> {
                cur.advance(); // skip the literal itself
                return ParseResult.ok(new FunctionColumn.Arg.Literal(Boolean.FALSE));
            }
            case NULL -> {
                cur.advance(); // skip the literal itself
                return ParseResult.ok(new FunctionColumn.Arg.Literal(null));
            }
            case STAR -> {
                cur.advance(); // skip the literal itself
                return ParseResult.ok(new FunctionColumn.Arg.Star());
            }
            default -> throw new ParserException("Unexpected token in function args: " + cur.peek().type(), cur.pos());
        }
    }
}
