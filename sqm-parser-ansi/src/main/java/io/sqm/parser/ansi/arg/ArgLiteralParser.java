package io.sqm.parser.ansi.arg;

import io.sqm.core.FunctionColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A parser used to parse literal as an argument in a function call.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT SUBSTRING('some string', 1, 3);
 *     }
 * </pre>
 */
public class ArgLiteralParser implements Parser<FunctionColumn.Arg.Literal> {

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
     * Parses function argument as a literal.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context.
     * @return {@link FunctionColumn.Arg.Literal}.
     */
    @Override
    public ParseResult<FunctionColumn.Arg.Literal> parse(Cursor cur, ParseContext ctx) {
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
            default -> {
                return error("Unexpected token in function args: " + cur.peek().type(), cur.fullPos());
            }
        }
    }

    /**
     * Gets {@link FunctionColumn.Arg.Literal} as a target type.
     *
     * @return {@link FunctionColumn.Arg.Literal}.
     */
    @Override
    public Class<FunctionColumn.Arg.Literal> targetType() {
        return FunctionColumn.Arg.Literal.class;
    }
}
