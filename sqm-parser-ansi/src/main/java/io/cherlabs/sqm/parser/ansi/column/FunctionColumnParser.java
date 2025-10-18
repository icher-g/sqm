package io.cherlabs.sqm.parser.ansi.column;

import io.cherlabs.sqm.core.FunctionColumn;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.copyOf;

public class FunctionColumnParser implements Parser<FunctionColumn> {

    private static String unescapeSqlString(String tokenLexeme) {
        // token lexeme is usually without surrounding quotes if Lexer strips them; if not, trim them first:
        String s = tokenLexeme;
        if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            s = s.substring(1, s.length() - 1);
        }
        // SQL escape of single quote is doubled: ''
        return s.replace("''", "'");
    }

    @Override
    public ParseResult<FunctionColumn> parse(Cursor cur, ParseContext ctx) {
        // parse the call starting at 0
        var funcResult = parseFunctionCall(cur, ctx);
        if (funcResult.isError()) {
            return error(funcResult);
        }

        // optional alias: AS identifier | bare identifier
        String alias = null;
        if (cur.consumeIf(TokenType.AS)) {
            if (!cur.match(TokenType.IDENT)) {
                return error("Expected alias after AS", cur.fullPos());
            }
            alias = cur.advance().lexeme();
        } else if (cur.match(TokenType.IDENT)) {
            alias = cur.advance().lexeme();
        }
        return ok(funcResult.value().as(alias));
    }

    @Override
    public Class<FunctionColumn> targetType() {
        return FunctionColumn.class;
    }

    /**
     * Parse ONLY a function call (no alias), starting at idx. Returns {FunctionColumn, nextIndex}.
     */
    private ParseResult<FunctionColumn> parseFunctionCall(Cursor cur, ParseContext ctx) {
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
                var ar = parseFunctionArg(cur, ctx); // parses one arg (may include nested function)
                if (ar.isError()) {
                    return error(ar);
                }
                args.add(ar.value());
                if (cur.consumeIf(TokenType.COMMA)) {
                    continue;
                }
                break;
            }
        }

        // ')'
        cur.expect("Expected ')' to close function", TokenType.RPAREN);
        return ok(new FunctionColumn(name.toString(), copyOf(args), distinct, null));
    }

    /**
     * Parse one function argument (column ref, literal, nested function, or '*').
     */
    private ParseResult<FunctionColumn.Arg> parseFunctionArg(Cursor cur, ParseContext ctx) {
        // Nested function: IDENT ('.' IDENT)* '(' ...
        if (ctx.lookups().looksLikeFunction(cur)) {
            var funcResult = parseFunctionCall(cur, ctx);
            if (funcResult.isError()) {
                return error(funcResult); // <— no alias, no EOF check here
            }
            return ok(new FunctionColumn.Arg.Function(funcResult.value()));
        }

        // ColumnRef: IDENT ('.' IDENT)?
        if (cur.match(TokenType.IDENT)) {
            String first = cur.advance().lexeme();
            if (cur.consumeIf(TokenType.DOT)) {
                var t = cur.expect("Expected identifier after '.'", TokenType.IDENT);
                String second = t.lexeme();
                return ok(new FunctionColumn.Arg.Column(first, second));
            } else {
                return ok(new FunctionColumn.Arg.Column(null, first));
            }
        }

        // Literals
        switch (cur.peek().type()) {
            case NUMBER -> {
                Object num = parseNumber(cur.advance().lexeme());
                return ok(new FunctionColumn.Arg.Literal(num));
            }
            case STRING -> {
                String s = unescapeSqlString(cur.advance().lexeme());
                return ok(new FunctionColumn.Arg.Literal(s));
            }
            case TRUE -> {
                cur.advance(); // skip the literal itself
                return ok(new FunctionColumn.Arg.Literal(Boolean.TRUE));
            }
            case FALSE -> {
                cur.advance(); // skip the literal itself
                return ok(new FunctionColumn.Arg.Literal(Boolean.FALSE));
            }
            case NULL -> {
                cur.advance(); // skip the literal itself
                return ok(new FunctionColumn.Arg.Literal(null));
            }
            case STAR -> {
                cur.advance(); // skip the literal itself
                return ok(new FunctionColumn.Arg.Star());
            }
            default -> {
                return error("Unexpected token in function args: " + cur.peek().type(), cur.fullPos());
            }
        }
    }
}
