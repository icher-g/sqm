package io.sqm.codegen;

import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CodegenSupportTypesTest {

    @Test
    void supportRecordsExposeConstructorState() {
        var lineColumn = new LineColumn(3, 14);
        assertEquals(3, lineColumn.line());
        assertEquals(14, lineColumn.column());

        ParseContext context = ParseContext.of(new AnsiSpecs());
        var parseStage = new ParseStage("ansi", context);
        assertEquals("ansi", parseStage.name());
        assertSame(context, parseStage.context());

        var statement = select(star()).from(tbl("users")).build();
        var parseResult = ParseResult.ok(statement);
        var quoting = IdentifierQuoting.of('"');
        var parseAttempt = new ParseAttempt("ansi", parseResult, quoting);
        assertEquals("ansi", parseAttempt.stage());
        assertSame(parseResult, parseAttempt.result());
        assertSame(quoting, parseAttempt.identifierQuoting());

        var user = Path.of("user");
        var relativePath = Path.of("user", "find_by_id.sql");
        var sqlSourceFile = new SqlSourceFile(
            relativePath,
            user,
            "findById",
            Set.of("id"),
            "hash-1",
            List.of(statement)
        );
        assertEquals(relativePath, sqlSourceFile.relativePath());
        assertEquals(user, sqlSourceFile.folder());
        assertEquals("findById", sqlSourceFile.methodName());
        assertSame(statement, sqlSourceFile.statements().getFirst());
        assertEquals(Set.of("id"), sqlSourceFile.parameters());
        assertEquals("hash-1", sqlSourceFile.sqlHash());

        var folderGroup = new SqlFolderGroup(user, "UserQueries", List.of(sqlSourceFile));
        assertEquals(user, folderGroup.folder());
        assertEquals("UserQueries", folderGroup.className());
        assertEquals(List.of(sqlSourceFile), folderGroup.files());
    }
}
