package io.sqm.parser.ansi.filter;

import io.sqm.core.CompositeFilter;
import io.sqm.core.Filter;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class CompositeFilterParser implements Parser<CompositeFilter> {

    private static CompositeFilter merge(CompositeFilter.Operator operator, Filter a, Filter b) {
        var list = new ArrayList<Filter>();
        if (a instanceof CompositeFilter ca && ca.op() == operator) {
            list.addAll(ca.filters());
        } else {
            list.add(a);
        }
        if (b instanceof CompositeFilter cb && cb.op() == operator) {
            list.addAll(cb.filters());
        } else {
            list.add(b);
        }
        return new CompositeFilter(operator, list);
    }

    @Override
    public ParseResult<CompositeFilter> parse(Cursor cur, ParseContext ctx) {
        // First handle OR.
        var tt = TokenType.OR;
        var i = cur.find(tt);

        // If not found look for AND.
        if (i == cur.size()) {
            tt = TokenType.AND;
            // avoid splitting by operator: BETWEEN 1 AND 2
            i = cur.find(Set.of(tt), Set.of(TokenType.BETWEEN, TokenType.LPAREN), Set.of(TokenType.AND, TokenType.RPAREN), 0);

            // If not found then it must be NOT.
            if (i == cur.size()) {
                cur.expect("Expect NOT in composite filter", TokenType.NOT);
                var filter = ctx.parse(Filter.class, cur);
                return ok(Filter.not(filter.value()));
            }
        }

        var left = ctx.parse(Filter.class, cur.advance(i));
        if (left.isError()) {
            return error(left);
        }

        var op = cur.expect("Expected " + tt, tt);

        var right = ctx.parse(Filter.class, cur);
        if (right.isError()) {
            return error(right);
        }
        return ok(merge(convert(op.lexeme(), cur), left.value(), right.value()));
    }

    @Override
    public Class<CompositeFilter> targetType() {
        return CompositeFilter.class;
    }

    private CompositeFilter.Operator convert(String op, Cursor cur) {
        switch (op.toLowerCase(Locale.ROOT)) {
            case "and" -> {
                return CompositeFilter.Operator.And;
            }
            case "or" -> {
                return CompositeFilter.Operator.Or;
            }
            case "not" -> {
                return CompositeFilter.Operator.Not;
            }
        }
        throw new ParserException("Unsupported operator: " + op, cur.fullPos());
    }
}
