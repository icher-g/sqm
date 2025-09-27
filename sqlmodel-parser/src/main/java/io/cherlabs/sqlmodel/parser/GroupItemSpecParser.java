package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.GroupItem;
import io.cherlabs.sqlmodel.parser.core.Cursor;

public class GroupItemSpecParser implements SpecParser<GroupItem> {
    private static boolean isPositiveInteger(String s) {
        // Fast path: all digits, no sign, no decimal.
        for (int i = 0, n = s.length(); i < n; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return !s.isEmpty();
    }

    @Override
    public Class<GroupItem> targetType() {
        return GroupItem.class;
    }

    @Override
    public ParseResult<GroupItem> parse(Cursor cur) {
        // Positional GROUP BY: "1", "2", ...
        // SQL allows positive 1-based ordinals.
        if (isPositiveInteger(cur.peek().lexeme())) {
            int pos = Integer.parseInt(cur.peek().lexeme());
            if (pos <= 0) {
                return ParseResult.error("GROUP BY position must be a positive integer", pos);
            }
            return ParseResult.ok(GroupItem.ofOrdinal(pos));
        }

        // Otherwise: delegate to the column parser
        var result = new ColumnSpecParser().parse(cur);
        if (!result.ok()) {
            return ParseResult.error(result);
        }
        return ParseResult.ok(GroupItem.of(result.value()));
    }
}
