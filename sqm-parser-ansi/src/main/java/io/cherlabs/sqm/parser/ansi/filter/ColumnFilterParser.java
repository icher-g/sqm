package io.cherlabs.sqm.parser.ansi.filter;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.ColumnFilter;
import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.parser.ansi.Terminators;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.ParserException;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

public class ColumnFilterParser implements Parser<ColumnFilter> {
    @Override
    public ParseResult<ColumnFilter> parse(Cursor cur, ParseContext ctx) {
        var column = ctx.parse(Column.class, cur.advance(cur.find(Terminators.COLUMN_IN_FILTER_TERMINATORS)));
        if (column.isError()) {
            return error(column);
        }

        if (cur.isEof()) {
            return ok(Filter.column(column.value()));
        }

        var op = parseOperator(cur);

        var values = ctx.parse(Values.class, cur);
        if (values.isError()) {
            return error(values);
        }
        return ok(new ColumnFilter(column.value(), op, values.value()));
    }

    @Override
    public Class<ColumnFilter> targetType() {
        return ColumnFilter.class;
    }

    private ColumnFilter.Operator parseOperator(Cursor cur) {
        if (cur.consumeIf(TokenType.IS)) {
            var isNot = cur.consumeIf(TokenType.NOT);
            if (cur.consumeIf(TokenType.NULL)) {
                return isNot ? ColumnFilter.Operator.IsNotNull : ColumnFilter.Operator.IsNull;
            }
            throw new ParserException("Unsupported operator: " + cur.peek().lexeme(), cur.fullPos());
        }

        if (cur.consumeIf(TokenType.NOT)) {
            if (cur.consumeIf(TokenType.IN)) {
                return ColumnFilter.Operator.NotIn;
            }
            if (cur.consumeIf(TokenType.LIKE)) {
                return ColumnFilter.Operator.NotLike;
            }
            throw new ParserException("Unsupported operator: " + cur.peek().lexeme(), cur.fullPos());
        }

        var t = cur.advance();
        switch (t.type()) {
            case IN -> {
                return ColumnFilter.Operator.In;
            }
            case BETWEEN -> {
                return ColumnFilter.Operator.Range;
            }
            case EQ -> {
                return ColumnFilter.Operator.Eq;
            }
            case NEQ1, NEQ2 -> {
                return ColumnFilter.Operator.Ne;
            }
            case LT -> {
                return ColumnFilter.Operator.Lt;
            }
            case LTE -> {
                return ColumnFilter.Operator.Lte;
            }
            case GT -> {
                return ColumnFilter.Operator.Gt;
            }
            case GTE -> {
                return ColumnFilter.Operator.Gte;
            }
            case LIKE -> {
                return ColumnFilter.Operator.Like;
            }
        }
        throw new ParserException("Unsupported operator: " + t.lexeme(), cur.fullPos());
    }
}
