package io.sqm.parser;

import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static org.junit.jupiter.api.Assertions.*;

class StatementSequenceParserTest {

    private static ParseContext contextWithSequenceParsers() {
        var repo = new DefaultParsersRepository()
            .register(StatementSequence.class, new StatementSequenceParser())
            .register(Statement.class, new StatementParser())
            .register(Query.class, new QueryStubParser())
            .register(InsertStatement.class, new InsertStubParser());
        return TestSupport.context(repo);
    }

    @Test
    void parsesMultipleStatements() {
        var ctx = contextWithSequenceParsers();

        var result = ctx.parse(StatementSequence.class, "SELECT; INSERT");

        assertTrue(result.ok());
        assertEquals(2, result.value().statements().size());
        assertInstanceOf(Query.class, result.value().statements().get(0));
        assertInstanceOf(InsertStatement.class, result.value().statements().get(1));
    }

    @Test
    void ignoresTrailingTerminator() {
        var ctx = contextWithSequenceParsers();

        var result = ctx.parse(StatementSequence.class, "SELECT;");

        assertTrue(result.ok());
        assertEquals(1, result.value().statements().size());
    }

    @Test
    void ignoresLeadingAndRepeatedEmptyStatements() {
        var ctx = contextWithSequenceParsers();

        var result = ctx.parse(StatementSequence.class, ";; SELECT;; INSERT ;;");

        assertTrue(result.ok());
        assertEquals(2, result.value().statements().size());
    }

    @Test
    void parsesOnlyTerminatorsAsEmptySequence() {
        var ctx = contextWithSequenceParsers();

        var result = ctx.parse(StatementSequence.class, ";;;");

        assertTrue(result.ok());
        assertTrue(result.value().statements().isEmpty());
    }

    @Test
    void preservesSemicolonsInsideLexicalTokens() {
        var ctx = contextWithSequenceParsers();

        var result = ctx.parse(StatementSequence.class, "SELECT ';'; SELECT $$a;b$$;");

        assertTrue(result.ok());
        assertEquals(2, result.value().statements().size());
    }

    @Test
    void propagatesStatementParseErrors() {
        var ctx = contextWithSequenceParsers();

        var result = ctx.parse(StatementSequence.class, "SELECT; CREATE");

        assertTrue(result.isError());
        assertEquals("Expected SELECT at 8", result.errorMessage());
    }

    private static final class QueryStubParser implements Parser<Query> {
        @Override
        public ParseResult<? extends Query> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected SELECT", TokenType.SELECT);
            if (cur.matchAny(TokenType.STRING, TokenType.DOLLAR_STRING)) {
                cur.advance();
            }
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
            cur.expect("Expected INSERT", TokenType.INSERT);
            return ParseResult.ok(insert("users").values(row(lit(1))).build());
        }

        @Override
        public Class<InsertStatement> targetType() {
            return InsertStatement.class;
        }
    }
}
