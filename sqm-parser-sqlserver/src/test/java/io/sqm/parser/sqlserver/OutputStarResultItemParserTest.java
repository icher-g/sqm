package io.sqm.parser.sqlserver;

import io.sqm.core.OutputRowSource;
import io.sqm.core.OutputStarResultItem;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputStarResultItemParserTest {

    @Test
    void parsesInsertedAndDeletedStarsCaseInsensitively() {
        var ctx = ParseContext.of(new SqlServerSpecs());

        var inserted = ctx.parse(OutputStarResultItem.class, "INSERTED.*");
        var deleted = ctx.parse(OutputStarResultItem.class, "deleted.*");

        assertTrue(inserted.ok(), inserted.errorMessage());
        assertEquals(OutputRowSource.INSERTED, inserted.value().source());
        assertTrue(deleted.ok(), deleted.errorMessage());
        assertEquals(OutputRowSource.DELETED, deleted.value().source());
    }

    @Test
    void errorsWhenPseudoRowSourceIsUnsupportedOrIncomplete() {
        var ctx = ParseContext.of(new SqlServerSpecs());

        var wrongSource = ctx.parse(OutputStarResultItem.class, "u.*");
        var missingStar = ctx.parse(OutputStarResultItem.class, "inserted.");

        assertTrue(wrongSource.isError());
        assertNotNull(wrongSource.errorMessage());
        assertTrue(missingStar.isError());
        assertNotNull(missingStar.errorMessage());
    }
}
