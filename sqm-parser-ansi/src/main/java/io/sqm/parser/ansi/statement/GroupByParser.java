package io.sqm.parser.ansi.statement;

import io.sqm.core.Group;
import io.sqm.core.GroupBy;
import io.sqm.parser.ansi.Terminators;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

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
