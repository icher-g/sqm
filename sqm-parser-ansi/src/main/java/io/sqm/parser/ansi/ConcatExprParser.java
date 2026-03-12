package io.sqm.parser.ansi;

import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.ConcatExpr;
import io.sqm.core.Expression;
import io.sqm.parser.ParserAdapter;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses ANSI string concatenation expressions using {@code ||}.
 */
public class ConcatExprParser implements Parser<Expression> {

    private final Parser<Expression> binaryParser;

    /**
     * Creates a concatenation-expression parser.
     */
    public ConcatExprParser() {
        /*
          The adapter is used to avoid casting issues when the returned expression
          is evaluated. The ParseResult returned by BinaryOperatorExprParser is expected
          to contain BinaryOperatorExpr but the actual value can be different.
         */
        this(ParserAdapter.widen(Expression.class, new BinaryOperatorExprParser()));
    }

    /**
     * Creates a concatenation-expression parser with a delegated binary parser.
     *
     * @param binaryParser binary-expression parser delegate
     */
    protected ConcatExprParser(Parser<Expression> binaryParser) {
        this.binaryParser = binaryParser;
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
        var expression = ctx.parse(binaryParser, cur);
        if (expression.isError()) {
            return error(expression);
        }

        if (expression.value() instanceof BinaryOperatorExpr binary && isConcat(binary)) {
            List<Expression> args = new ArrayList<>();
            collectArgs(binary, args);
            return ok(ConcatExpr.of(args));
        }

        return ok(expression.value());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ConcatExpr> targetType() {
        return ConcatExpr.class;
    }

    private void collectArgs(Expression expression, List<Expression> args) {
        if (expression instanceof BinaryOperatorExpr binary && isConcat(binary)) {
            collectArgs(binary.left(), args);
            collectArgs(binary.right(), args);
            return;
        }
        args.add(expression);
    }

    private boolean isConcat(BinaryOperatorExpr binary) {
        return !binary.operator().operatorKeywordSyntax() && "||".equals(binary.operator().symbol());
    }
}
