package io.sqm.render.sqlserver;

import io.sqm.core.TopSpec;
import io.sqm.render.SqlWriter;

final class SqlServerTopSpecRenderSupport {
    private SqlServerTopSpecRenderSupport() {
    }

    static void renderTopSpec(TopSpec topSpec, SqlWriter w) {
        w.space().append("TOP (");
        w.append(topSpec.count());
        w.append(")");
        if (topSpec.percent()) {
            w.space().append("PERCENT");
        }
        if (topSpec.withTies()) {
            w.space().append("WITH TIES");
        }
    }
}
