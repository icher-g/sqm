package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.PivotMeasure;
import io.sqm.core.PivotTable;
import io.sqm.core.PivotValue;
import io.sqm.core.TableRef;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses relational {@code PIVOT (...)} table transforms.
 */
public class PivotTableParser implements MatchableParser<PivotTable>, InfixParser<TableRef, PivotTable> {
    /**
     * Creates a pivot table parser.
     */
    public PivotTableParser() {
    }

    /**
     * Parses a standalone pivot table.
     *
     * @param cur cursor positioned at {@code PIVOT}
     * @param ctx parse context
     * @return error because pivot requires a left source relation
     */
    @Override
    public ParseResult<? extends PivotTable> parse(Cursor cur, ParseContext ctx) {
        return error("PIVOT requires a source table", cur.fullPos());
    }

    /**
     * Parses {@code source PIVOT (...)}.
     *
     * @param source source table reference
     * @param cur cursor positioned at {@code PIVOT}
     * @param ctx parse context
     * @return pivot table
     */
    @Override
    public ParseResult<PivotTable> parse(TableRef source, Cursor cur, ParseContext ctx) {
        cur.expect("Expected PIVOT", TokenType.PIVOT);
        if (!ctx.capabilities().supports(SqlFeature.PIVOT_TABLE)) {
            return error("PIVOT is not supported by this dialect", cur.fullPos());
        }
        if (cur.match(TokenType.IDENT) && "XML".equalsIgnoreCase(cur.peek().lexeme())) {
            cur.advance();
            return error("PIVOT XML is not supported", cur.fullPos());
        }

        cur.expect("Expected '(' after PIVOT", TokenType.LPAREN);
        var measures = parseItems(PivotMeasure.class, cur, ctx);
        if (measures.isError()) {
            return error(measures);
        }
        cur.expect("Expected FOR in PIVOT", TokenType.FOR);
        var forExpression = ctx.parse(Expression.class, cur);
        if (forExpression.isError()) {
            return error(forExpression);
        }
        cur.expect("Expected IN in PIVOT", TokenType.IN);
        cur.expect("Expected '(' after PIVOT IN", TokenType.LPAREN);
        var values = parseItems(PivotValue.class, cur, ctx);
        if (values.isError()) {
            return error(values);
        }
        cur.expect("Expected ')' after PIVOT values", TokenType.RPAREN);
        cur.expect("Expected ')' after PIVOT", TokenType.RPAREN);

        return ok(PivotTable.of(source, measures.value(), forExpression.value(), values.value(), parseAliasIdentifier(cur)));
    }

    /**
     * Gets the parser target type.
     *
     * @return pivot table type
     */
    @Override
    public Class<PivotTable> targetType() {
        return PivotTable.class;
    }

    /**
     * Checks whether the current token starts a pivot transform.
     *
     * @param cur cursor
     * @param ctx parse context
     * @return {@code true} when current token is {@code PIVOT}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.PIVOT);
    }
}
