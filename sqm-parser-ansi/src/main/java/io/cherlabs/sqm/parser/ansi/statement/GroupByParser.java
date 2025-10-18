package io.cherlabs.sqm.parser.ansi.statement;

import io.cherlabs.sqm.core.Group;
import io.cherlabs.sqm.core.GroupBy;
import io.cherlabs.sqm.parser.ansi.Terminators;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class GroupByParser implements Parser<GroupBy> {

    @Override
    public ParseResult<GroupBy> parse(Cursor cur, ParseContext ctx) {
        // GROUP BY (optional)
        List<Group> items = new ArrayList<>();
        if (cur.consumeIf(TokenType.GROUP)) {
            // expect BY
            cur.expect("Expected BY after GROUP", TokenType.BY);

            var groupCur = cur.advance(cur.find(Terminators.GROUP_TERMINATORS));
            while (!groupCur.isEof()) {
                Cursor itemCur = groupCur.advance(groupCur.find(Terminators.ITEM_TERMINATORS));
                var gr = ctx.parse(Group.class, itemCur);
                if (gr.isError()) {
                    return error(gr);
                }
                items.add(gr.value());
                groupCur.consumeIf(TokenType.COMMA);
            }
        }
        return ok(new GroupBy(items));
    }

    @Override
    public Class<GroupBy> targetType() {
        return GroupBy.class;
    }
}
