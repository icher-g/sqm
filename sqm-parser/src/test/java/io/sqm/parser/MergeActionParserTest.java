package io.sqm.parser;

import io.sqm.core.MergeAction;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeUpdateAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeActionParserTest {

    private static ParseContext contextWithMergeActionParsers(
        Parser<MergeUpdateAction> updateParser,
        Parser<MergeDeleteAction> deleteParser,
        Parser<MergeInsertAction> insertParser
    ) {
        var repo = new DefaultParsersRepository()
            .register(MergeAction.class, new MergeActionParser())
            .register(MergeUpdateAction.class, updateParser)
            .register(MergeDeleteAction.class, deleteParser)
            .register(MergeInsertAction.class, insertParser);
        return TestSupport.context(repo);
    }

    @Test
    void delegatesToUpdateActionParser() {
        var ctx = contextWithMergeActionParsers(
            new UpdateActionStubParser(),
            new DeleteActionStubParser(),
            new InsertActionStubParser()
        );

        var result = ctx.parse(MergeAction.class, "UPDATE");

        assertTrue(result.ok());
        assertInstanceOf(MergeUpdateAction.class, result.value());
    }

    @Test
    void delegatesToDeleteActionParser() {
        var ctx = contextWithMergeActionParsers(
            new UpdateActionStubParser(),
            new DeleteActionStubParser(),
            new InsertActionStubParser()
        );

        var result = ctx.parse(MergeAction.class, "DELETE");

        assertTrue(result.ok());
        assertInstanceOf(MergeDeleteAction.class, result.value());
    }

    @Test
    void delegatesToInsertActionParser() {
        var ctx = contextWithMergeActionParsers(
            new UpdateActionStubParser(),
            new DeleteActionStubParser(),
            new InsertActionStubParser()
        );

        var result = ctx.parse(MergeAction.class, "INSERT");

        assertTrue(result.ok());
        assertInstanceOf(MergeInsertAction.class, result.value());
    }

    @Test
    void reportsErrorForUnknownMergeActionKeyword() {
        var result = contextWithMergeActionParsers(
            new UpdateActionStubParser(),
            new DeleteActionStubParser(),
            new InsertActionStubParser()
        ).parse(MergeAction.class, "SELECT");

        assertTrue(result.isError());
    }

    private static final class UpdateActionStubParser implements Parser<MergeUpdateAction> {
        @Override
        public ParseResult<? extends MergeUpdateAction> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected UPDATE", io.sqm.parser.core.TokenType.UPDATE);
            return ParseResult.ok(io.sqm.core.MergeUpdateAction.of(java.util.List.of(io.sqm.dsl.Dsl.set("name", io.sqm.dsl.Dsl.lit("alice")))));
        }

        @Override
        public Class<MergeUpdateAction> targetType() {
            return MergeUpdateAction.class;
        }
    }

    private static final class DeleteActionStubParser implements Parser<MergeDeleteAction> {
        @Override
        public ParseResult<? extends MergeDeleteAction> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected DELETE", io.sqm.parser.core.TokenType.DELETE);
            return ParseResult.ok(io.sqm.core.MergeDeleteAction.of());
        }

        @Override
        public Class<MergeDeleteAction> targetType() {
            return MergeDeleteAction.class;
        }
    }

    private static final class InsertActionStubParser implements Parser<MergeInsertAction> {
        @Override
        public ParseResult<? extends MergeInsertAction> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected INSERT", io.sqm.parser.core.TokenType.INSERT);
            return ParseResult.ok(io.sqm.core.MergeInsertAction.of(java.util.List.of(), io.sqm.dsl.Dsl.row(io.sqm.dsl.Dsl.lit(1))));
        }

        @Override
        public Class<MergeInsertAction> targetType() {
            return MergeInsertAction.class;
        }
    }
}
