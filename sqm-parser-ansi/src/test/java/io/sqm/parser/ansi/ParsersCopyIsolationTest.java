package io.sqm.parser.ansi;

import io.sqm.core.LimitOffset;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ParsersCopyIsolationTest {

    @Test
    void ansiCopy_returns_isolated_repository() {
        var shared = Parsers.ansi();
        var copy = Parsers.ansiCopy();

        assertNotSame(shared, copy);
        assertInstanceOf(LimitOffsetParser.class, shared.require(LimitOffset.class));
        assertInstanceOf(LimitOffsetParser.class, copy.require(LimitOffset.class));

        copy.register(new Parser<LimitOffset>() {
            @Override
            public ParseResult<LimitOffset> parse(Cursor cur, ParseContext ctx) {
                return ParseResult.error("forced", cur.fullPos());
            }

            @Override
            public Class<LimitOffset> targetType() {
                return LimitOffset.class;
            }
        });

        assertInstanceOf(LimitOffsetParser.class, shared.require(LimitOffset.class));
    }
}

