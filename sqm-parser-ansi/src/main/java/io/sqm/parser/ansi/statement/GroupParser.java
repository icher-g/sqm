package io.sqm.parser.ansi.statement;

import io.sqm.core.Column;
import io.sqm.core.Group;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A spec parser for group by item specifications.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     "u.user_name", "o.status"
 *     "1", "2", "3" // ordinal group by.
 *     }
 * </pre>
 */
public class GroupParser implements Parser<Group> {

    private static boolean isPositiveInteger(String s) {
        // Fast path: all digits, no sign, no decimal.
        for (int i = 0, n = s.length(); i < n; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return !s.isEmpty();
    }

    /**
     * Gets the {@link Group} type.
     *
     * @return {@link Group} type.
     */
    @Override
    public Class<Group> targetType() {
        return Group.class;
    }

    /**
     * Parses the group by item specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
    @Override
    public ParseResult<Group> parse(Cursor cur, ParseContext ctx) {
        // Positional GROUP BY: "1", "2", ...
        // SQL allows positive 1-based ordinals.
        if (isPositiveInteger(cur.peek().lexeme())) {
            int pos = Integer.parseInt(cur.peek().lexeme());
            if (pos <= 0) {
                return ParseResult.error("GROUP BY position must be a positive integer", pos);
            }
            return ParseResult.ok(Group.by(pos));
        }

        // Otherwise: delegate to the column parser
        var result = ctx.parse(Column.class, cur);
        if (result.isError()) {
            return ParseResult.error(result);
        }
        return ParseResult.ok(Group.by(result.value()));
    }
}
