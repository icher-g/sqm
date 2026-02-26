package io.sqm.parser.ansi;

import io.sqm.core.FrameSpec;
import io.sqm.core.OrderBy;
import io.sqm.core.OverSpec;
import io.sqm.core.PartitionBy;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses inline {@code OVER (...)} window specifications.
 */
public class OverSpecDefParser implements Parser<OverSpec.Def> {
    /**
     * Creates an over-spec definition parser.
     */
    public OverSpecDefParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OverSpec.Def> parse(Cursor cur, ParseContext ctx) {
        io.sqm.core.Identifier baseWindow = null;
        if (cur.match(TokenType.IDENT)) {
            baseWindow = toIdentifier(cur.advance());
        }

        PartitionBy partitionBy = null;
        if (cur.match(TokenType.PARTITION)) {
            var pr = ctx.parse(PartitionBy.class, cur);
            if (pr.isError()) {
                return error(pr);
            }
            partitionBy = pr.value();
        }

        OrderBy orderBy = null;
        if (cur.match(TokenType.ORDER)) {
            var or = ctx.parse(OrderBy.class, cur);
            if (or.isError()) {
                return error(or);
            }
            orderBy = or.value();
        }

        FrameSpec frame = null;
        if (cur.matchAny(TokenType.ROWS, TokenType.RANGE, TokenType.GROUPS)) {
            var fr = ctx.parse(FrameSpec.class, cur);
            if (fr.isError()) {
                return error(fr);
            }
            frame = fr.value();
        }

        OverSpec.Exclude exclude = null;
        if (cur.consumeIf(TokenType.EXCLUDE)) {
            if (cur.matchAny(TokenType.TIES, TokenType.GROUP)) {
                exclude = Enum.valueOf(OverSpec.Exclude.class, cur.peek().lexeme());
                cur.advance();
            }
            else {
                if (cur.consumeIf(TokenType.CURRENT)) {
                    cur.expect("Expected ROW after CURRENT", TokenType.ROW);
                    exclude = OverSpec.Exclude.CURRENT_ROW;
                }
                else
                    if (cur.consumeIf(TokenType.NO)) {
                        cur.expect("Expected OTHERS after NO", TokenType.OTHERS);
                        exclude = OverSpec.Exclude.NO_OTHERS;
                    }
            }
        }

        if (baseWindow == null) {
            return ok(OverSpec.def(partitionBy, orderBy, frame, exclude));
        }
        return ok(OverSpec.def(baseWindow, orderBy, frame, exclude));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OverSpec.Def> targetType() {
        return OverSpec.Def.class;
    }
}
