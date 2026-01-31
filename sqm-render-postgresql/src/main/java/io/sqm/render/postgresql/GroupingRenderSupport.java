package io.sqm.render.postgresql;

import io.sqm.core.GroupItem;
import io.sqm.render.SqlWriter;

import java.util.List;

final class GroupingRenderSupport {
    private GroupingRenderSupport() {
    }

    static void renderGroupingContainer(List<GroupItem> items, SqlWriter w, boolean spaceBeforeParen) {
        if (spaceBeforeParen) {
            w.space();
        }
        w.append("(");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                w.append(",").space();
            }
            w.append(items.get(i));
        }
        w.append(")");
    }
}
