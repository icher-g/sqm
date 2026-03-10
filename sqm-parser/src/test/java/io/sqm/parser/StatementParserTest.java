package io.sqm.parser;

import io.sqm.core.DeleteStatement;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementParserTest {

    private static ParseContext contextWithStatementParsers() {
        var repo = new DefaultParsersRepository()
            .register(Statement.class, new StatementParser())
            .register(Query.class, new QueryStubParser())
            .register(InsertStatement.class, new InsertStubParser())
            .register(UpdateStatement.class, new UpdateStubParser())
            .register(DeleteStatement.class, new DeleteStubParser());
        return TestSupport.context(repo);
    }

    @Test
    void delegatesToQueryParser() {
        var ctx = contextWithStatementParsers();
        var result = ctx.parse(Statement.class, "SELECT");

        assertTrue(result.ok());
        assertInstanceOf(Query.class, result.value());
    }

    @Test
    void delegatesToInsertParser() {
        var ctx = contextWithStatementParsers();
        var result = ctx.parse(Statement.class, "INSERT");

        assertTrue(result.ok());
        assertInstanceOf(InsertStatement.class, result.value());
    }

    @Test
    void delegatesReplaceToInsertParser() {
        var ctx = contextWithStatementParsers();
        var result = ctx.parse(Statement.class, "REPLACE");

        assertTrue(result.ok());
        assertInstanceOf(InsertStatement.class, result.value());
    }

    @Test
    void delegatesToUpdateParser() {
        var ctx = contextWithStatementParsers();
        var result = ctx.parse(Statement.class, "UPDATE");

        assertTrue(result.ok());
        assertInstanceOf(UpdateStatement.class, result.value());
    }

    @Test
    void delegatesToDeleteParser() {
        var ctx = contextWithStatementParsers();
        var result = ctx.parse(Statement.class, "DELETE");

        assertTrue(result.ok());
        assertInstanceOf(DeleteStatement.class, result.value());
    }

    @Test
    void returnsErrorForUnsupportedStatementStart() {
        var ctx = contextWithStatementParsers();
        var result = ctx.parse(Statement.class, "MERGE INTO users");

        assertTrue(result.isError());
        assertEquals("Expected SELECT at 0", result.errorMessage());
    }

    private static final class QueryStubParser implements Parser<Query> {
        @Override
        public ParseResult<? extends Query> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected SELECT", TokenType.SELECT);
            return ParseResult.ok(Query.select().build());
        }

        @Override
        public Class<Query> targetType() {
            return Query.class;
        }
    }

    private static final class InsertStubParser implements Parser<InsertStatement> {
        @Override
        public ParseResult<? extends InsertStatement> parse(Cursor cur, ParseContext ctx) {
            if (cur.match(TokenType.REPLACE)) {
                cur.expect("Expected REPLACE", TokenType.REPLACE);
                return ParseResult.ok(io.sqm.dsl.Dsl.insert("users").replace().values(io.sqm.dsl.Dsl.row(io.sqm.dsl.Dsl.lit(1))).build());
            }
            cur.expect("Expected INSERT", TokenType.INSERT);
            return ParseResult.ok(io.sqm.dsl.Dsl.insert("users").values(io.sqm.dsl.Dsl.row(io.sqm.dsl.Dsl.lit(1))).build());
        }

        @Override
        public Class<InsertStatement> targetType() {
            return InsertStatement.class;
        }
    }

    private static final class UpdateStubParser implements Parser<UpdateStatement> {
        @Override
        public ParseResult<? extends UpdateStatement> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected UPDATE", TokenType.UPDATE);
            return ParseResult.ok(io.sqm.dsl.Dsl.update("users").set(io.sqm.dsl.Dsl.id("name"), io.sqm.dsl.Dsl.lit("alice")).build());
        }

        @Override
        public Class<UpdateStatement> targetType() {
            return UpdateStatement.class;
        }
    }

    private static final class DeleteStubParser implements Parser<DeleteStatement> {
        @Override
        public ParseResult<? extends DeleteStatement> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected DELETE", TokenType.DELETE);
            return ParseResult.ok(io.sqm.dsl.Dsl.delete("users").build());
        }

        @Override
        public Class<DeleteStatement> targetType() {
            return DeleteStatement.class;
        }
    }
}
