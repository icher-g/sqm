package io.sqm.parser.ansi.statement;

import io.sqm.core.LimitOffset;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class LimitOffsetParser implements Parser<LimitOffset> {

    @Override
    public ParseResult<LimitOffset> parse(Cursor cur, ParseContext ctx) {
        // LIMIT (optional) — numeric only
        Long limit = null;
        if (cur.consumeIf(TokenType.LIMIT)) {
            if (!cur.match(TokenType.NUMBER)) {
                return error("Expected number after LIMIT", cur.fullPos());
            }
            var t = cur.advance();
            limit = (long) Double.parseDouble(t.lexeme());
        }

        // ANSI FETCH (can appear with or without OFFSET)
        if (cur.consumeIf(TokenType.FETCH)) {
            var fr = parseOptionalFetchClause(cur);
            if (fr.isError()) {
                return error(fr);
            }
            limit = fr.value();
        }

        // OFFSET (optional) — numeric only
        Long offset = null;
        if (cur.consumeIf(TokenType.OFFSET)) {
            if (!cur.match(TokenType.NUMBER)) {
                return error("Expected number after OFFSET", cur.fullPos());
            }
            var t = cur.advance();
            offset = (long) Double.parseDouble(t.lexeme());

            // Optional ROW / ROWS
            if (cur.match(TokenType.ROW) || cur.match(TokenType.ROWS)) {
                cur.advance();
            }

            // Optional ANSI FETCH after OFFSET
            if (cur.consumeIf(TokenType.FETCH)) {
                var fr = parseOptionalFetchClause(cur);
                if (fr.isError()) {
                    return error(fr);
                }
                limit = fr.value();
            }
        }

        return ok(new LimitOffset(limit, offset));
    }

    @Override
    public Class<LimitOffset> targetType() {
        return LimitOffset.class;
    }

    private ParseResult<Long> parseOptionalFetchClause(Cursor cur) {
        // FETCH requires FIRST or NEXT
        if (!(cur.consumeIf(TokenType.FIRST) || cur.consumeIf(TokenType.NEXT))) {
            return error("Expected FIRST or NEXT after FETCH", cur.fullPos());
        }

        if (!cur.match(TokenType.NUMBER)) {
            return error("Expected number after FETCH FIRST/NEXT", cur.fullPos());
        }

        var t = cur.advance();
        var limit = (long) Double.parseDouble(t.lexeme());

        // Optional ROW / ROWS
        if (cur.match(TokenType.ROW) || cur.match(TokenType.ROWS)) {
            cur.advance();
        }

        if (!cur.consumeIf(TokenType.ONLY)) {
            return error("Expected ONLY at the end of FETCH clause", cur.fullPos());
        }
        return ok(limit);
    }
}
