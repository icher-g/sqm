package io.sqm.parser.ansi;

import io.sqm.core.TimeZoneSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;

final class TemporalLiteralParsingSupport {
    private TemporalLiteralParsingSupport() {
    }

    static int skipTimeZoneSpec(Cursor cur, int offset) {
        if (!cur.matchAny(offset, TokenType.WITH, TokenType.WITHOUT)) {
            return offset;
        }
        int p = offset + 1;
        if (!cur.match(TokenType.IDENT, "time", p)) {
            return -1;
        }
        p++;
        if (!cur.match(TokenType.IDENT, "zone", p)) {
            return -1;
        }
        return p + 1;
    }

    static TimeZoneSpec parseTimeZoneSpec(Cursor cur) {
        if (!cur.matchAny(TokenType.WITH, TokenType.WITHOUT)) {
            return TimeZoneSpec.NONE;
        }
        var keyword = cur.advance().lexeme();
        var time = cur.expect("Expected identifier 'time'", TokenType.IDENT);
        if (!time.lexeme().equalsIgnoreCase("time")) {
            throw new ParserException("Expected 'time' but found '" + time.lexeme() + "'", cur.fullPos());
        }
        var zone = cur.expect("Expected identifier 'zone'", TokenType.IDENT);
        if (!zone.lexeme().equalsIgnoreCase("zone")) {
            throw new ParserException("Expected 'zone' but found '" + zone.lexeme() + "'", cur.fullPos());
        }
        return keyword.equalsIgnoreCase("with") ? TimeZoneSpec.WITH_TIME_ZONE : TimeZoneSpec.WITHOUT_TIME_ZONE;
    }
}
