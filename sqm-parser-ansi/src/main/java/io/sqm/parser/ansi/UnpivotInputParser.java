package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.Identifier;
import io.sqm.core.UnpivotInput;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses an {@link UnpivotInput} branch inside an {@code UNPIVOT} clause.
 */
public class UnpivotInputParser implements Parser<UnpivotInput> {
    /**
     * Creates an unpivot input parser.
     */
    public UnpivotInputParser() {
    }

    /**
     * Parses an unpivot input branch.
     *
     * @param cur cursor positioned at the source column group
     * @param ctx parse context
     * @return parsed unpivot input
     */
    @Override
    public ParseResult<? extends UnpivotInput> parse(Cursor cur, ParseContext ctx) {
        var sourceColumns = parseColumnGroup(cur);
        var label = parseLabel(cur, ctx);
        if (label.isError()) {
            return error(label);
        }
        Expression labelExpression = label.value();
        if (labelExpression == null) {
            labelExpression = Expression.literal(sourceColumns.getFirst().value());
        }
        return ok(UnpivotInput.of(sourceColumns, labelExpression));
    }

    /**
     * Gets the parser target type.
     *
     * @return unpivot input type
     */
    @Override
    public Class<UnpivotInput> targetType() {
        return UnpivotInput.class;
    }

    /**
     * Parses an optional unpivot input label.
     *
     * @param cur cursor positioned after the source column group
     * @param ctx parse context
     * @return parsed label or {@code null}
     */
    protected ParseResult<Expression> parseLabel(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.AS)) {
            var parsedLabel = ctx.parse(Expression.class, cur);
            if (parsedLabel.isError()) {
                return error(parsedLabel);
            }
            return ok(parsedLabel.value());
        }
        return ok(null);
    }

    private List<Identifier> parseColumnGroup(Cursor cur) {
        if (!cur.consumeIf(TokenType.LPAREN)) {
            return List.of(toIdentifier(cur.expect("Expected UNPIVOT input column", TokenType.IDENT)));
        }
        var columns = parseIdentifierItems(cur, "Expected UNPIVOT input column");
        cur.expect("Expected ')' after UNPIVOT input columns", TokenType.RPAREN);
        return columns;
    }
}
