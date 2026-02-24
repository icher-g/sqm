package io.sqm.render.ansi;

import io.sqm.core.Identifier;
import io.sqm.core.QuoteStyle;
import io.sqm.core.Table;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.dsl.Dsl;
import io.sqm.render.SqlWriter;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableRendererTest {

    private static String render(Table table) {
        RenderContext ctx = RenderContext.of(new AnsiDialect());
        SqlWriter w = new DefaultSqlWriter(ctx);
        w.append(table);
        return w.toText(List.of()).sql();
    }

    @Test
    @DisplayName("Renders simple table")
    void renders_table() {
        var sql = render(Dsl.tbl("t").as("a"));
        assertTrue(sql.contains("t"));
        assertTrue(sql.contains("AS a"));
    }

    @Test
    @DisplayName("Rejects ONLY in ANSI renderer")
    void rejects_only() {
        var table = Dsl.tbl("t").only();
        assertThrows(UnsupportedDialectFeatureException.class, () -> render(table));
    }

    @Test
    @DisplayName("Rejects table inheritance star in ANSI renderer")
    void rejects_inheritance_star() {
        var table = Dsl.tbl("t").includingDescendants();
        assertThrows(UnsupportedDialectFeatureException.class, () -> render(table));
    }

    @Test
    @DisplayName("Preserves double-quoted identifiers")
    void preserves_double_quoted_identifiers() {
        var sql = render(Table.of(
            Identifier.of("Sales", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("Users", QuoteStyle.DOUBLE_QUOTE),
            Identifier.of("U", QuoteStyle.DOUBLE_QUOTE),
            Table.Inheritance.DEFAULT
        ));

        assertTrue(sql.contains("\"Sales\".\"Users\""));
        assertTrue(sql.contains("AS \"U\""));
    }

    @Test
    @DisplayName("Converts unsupported quote styles to dialect default quotes")
    void converts_unsupported_quote_styles_to_dialect_default() {
        var sql = render(Table.of(
            Identifier.of("db", QuoteStyle.BRACKETS),
            Identifier.of("user", QuoteStyle.BACKTICK),
            Identifier.of("u", QuoteStyle.BACKTICK),
            Table.Inheritance.DEFAULT
        ));

        assertTrue(sql.contains("\"db\".\"user\""));
        assertTrue(sql.contains("AS \"u\""));
    }
}
