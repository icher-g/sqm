package io.sqm.parser;

import io.sqm.core.GroupItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupItemParserTest {

    private static ParseContext contextWithParsers() {
        var repo = new DefaultParsersRepository()
            .register(GroupItem.class, new GroupItemParser())
            .register(GroupItem.GroupingSets.class, new GroupingSetsParser())
            .register(GroupItem.Rollup.class, new RollupParser())
            .register(GroupItem.Cube.class, new CubeParser())
            .register(GroupItem.GroupingSet.class, new GroupingSetParser())
            .register(GroupItem.SimpleGroupItem.class, new SimpleGroupItemParser());
        return TestSupport.context(repo);
    }

    @Test
    void parsesGroupingSets() {
        var ctx = contextWithParsers();
        var result = ctx.parse(GroupItem.class, "sets");

        assertTrue(result.ok());
        assertInstanceOf(GroupItem.GroupingSets.class, result.value());
    }

    @Test
    void parsesRollup() {
        var ctx = contextWithParsers();
        var result = ctx.parse(GroupItem.class, "rollup");

        assertTrue(result.ok());
        assertInstanceOf(GroupItem.Rollup.class, result.value());
    }

    @Test
    void parsesCube() {
        var ctx = contextWithParsers();
        var result = ctx.parse(GroupItem.class, "cube");

        assertTrue(result.ok());
        assertInstanceOf(GroupItem.Cube.class, result.value());
    }

    @Test
    void parsesGroupingSet() {
        var ctx = contextWithParsers();
        var result = ctx.parse(GroupItem.class, "set");

        assertTrue(result.ok());
        assertInstanceOf(GroupItem.GroupingSet.class, result.value());
    }

    @Test
    void fallsBackToSimpleGroupItem() {
        var ctx = contextWithParsers();
        var result = ctx.parse(GroupItem.class, "simple");

        assertTrue(result.ok());
        assertInstanceOf(GroupItem.SimpleGroupItem.class, result.value());
    }

    private static final class GroupingSetsParser implements MatchableParser<GroupItem.GroupingSets> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.SETS);
        }

        @Override
        public ParseResult<? extends GroupItem.GroupingSets> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok((GroupItem.GroupingSets) GroupItem.groupingSets());
        }

        @Override
        public Class<? extends GroupItem.GroupingSets> targetType() {
            return GroupItem.GroupingSets.class;
        }
    }

    private static final class RollupParser implements MatchableParser<GroupItem.Rollup> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.ROLLUP);
        }

        @Override
        public ParseResult<? extends GroupItem.Rollup> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok((GroupItem.Rollup) GroupItem.rollup());
        }

        @Override
        public Class<? extends GroupItem.Rollup> targetType() {
            return GroupItem.Rollup.class;
        }
    }

    private static final class CubeParser implements MatchableParser<GroupItem.Cube> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.CUBE);
        }

        @Override
        public ParseResult<? extends GroupItem.Cube> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok((GroupItem.Cube) GroupItem.cube());
        }

        @Override
        public Class<? extends GroupItem.Cube> targetType() {
            return GroupItem.Cube.class;
        }
    }

    private static final class GroupingSetParser implements MatchableParser<GroupItem.GroupingSet> {
        @Override
        public boolean match(Cursor cur, ParseContext ctx) {
            return cur.match(TokenType.IDENT) && "set".equals(cur.peek().lexeme());
        }

        @Override
        public ParseResult<? extends GroupItem.GroupingSet> parse(Cursor cur, ParseContext ctx) {
            cur.advance();
            return ParseResult.ok((GroupItem.GroupingSet) GroupItem.groupingSet());
        }

        @Override
        public Class<? extends GroupItem.GroupingSet> targetType() {
            return GroupItem.GroupingSet.class;
        }
    }

    private static final class SimpleGroupItemParser implements Parser<GroupItem.SimpleGroupItem> {
        @Override
        public ParseResult<? extends GroupItem.SimpleGroupItem> parse(Cursor cur, ParseContext ctx) {
            var token = cur.expect("Expected identifier", TokenType.IDENT);
            return ParseResult.ok(GroupItem.of(col(token.lexeme())));
        }

        @Override
        public Class<GroupItem.SimpleGroupItem> targetType() {
            return GroupItem.SimpleGroupItem.class;
        }
    }
}

