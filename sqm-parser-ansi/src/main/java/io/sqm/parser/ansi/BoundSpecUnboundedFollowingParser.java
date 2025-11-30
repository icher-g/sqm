package io.sqm.parser.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.ok;

public class BoundSpecUnboundedFollowingParser implements Parser<BoundSpec.UnboundedFollowing> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<BoundSpec.UnboundedFollowing> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected UNBOUNDED", TokenType.UNBOUNDED);
        cur.expect("Expected FOLLOWING after UNBOUNDED", TokenType.FOLLOWING);
        return ok(BoundSpec.unboundedFollowing());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BoundSpec.UnboundedFollowing> targetType() {
        return BoundSpec.UnboundedFollowing.class;
    }
}
