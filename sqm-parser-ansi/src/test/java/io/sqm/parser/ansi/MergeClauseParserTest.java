package io.sqm.parser.ansi;

import io.sqm.core.MergeClause;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeUpdateAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.OperatorPolicy;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.spi.Specs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeClauseParserTest {

    @Test
    void rejectsMergeClauseByDefault() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(MergeClause.class, "WHEN MATCHED THEN DELETE");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("not supported"));
    }

    @Test
    void parsesSharedMergeClauseShapesThroughSupportedHook() {
        var ctx = ParseContext.of(new SupportedMergeClauseSpecs());
        var parser = new SupportedMergeClauseParser();

        var matched = ctx.parse(parser, "WHEN MATCHED AND src.active = true THEN DELETE");
        var notMatched = ctx.parse(parser, "WHEN NOT MATCHED THEN INSERT VALUES (src.id)");
        var bySource = ctx.parse(parser, "WHEN NOT MATCHED BY SOURCE AND users.active = true THEN UPDATE SET name = src.name");

        assertTrue(matched.ok(), matched.errorMessage());
        assertEquals(MergeClause.MatchType.MATCHED, matched.value().matchType());
        assertNotNull(matched.value().condition());

        assertTrue(notMatched.ok(), notMatched.errorMessage());
        assertEquals(MergeClause.MatchType.NOT_MATCHED, notMatched.value().matchType());

        assertTrue(bySource.ok(), bySource.errorMessage());
        assertEquals(MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, bySource.value().matchType());
        assertNotNull(bySource.value().condition());
    }

    @Test
    void rejectsUnsupportedNotMatchedByFamilyAndInvalidActionShapes() {
        var ctx = ParseContext.of(new SupportedMergeClauseSpecs());

        var unsupportedBy = ctx.parse(new UnsupportedByMergeClauseParser(), "WHEN NOT MATCHED BY SOURCE THEN DELETE");
        var matchedInsert = ctx.parse(new SupportedMergeClauseParser(), "WHEN MATCHED THEN INSERT VALUES (src.id)");
        var notMatchedDelete = ctx.parse(new SupportedMergeClauseParser(), "WHEN NOT MATCHED THEN DELETE");
        var bySourceInsert = ctx.parse(new SupportedMergeClauseParser(), "WHEN NOT MATCHED BY SOURCE THEN INSERT VALUES (src.id)");

        assertTrue(unsupportedBy.isError());
        assertTrue(Objects.requireNonNull(unsupportedBy.errorMessage()).contains("WHEN NOT MATCHED BY"));
        assertTrue(matchedInsert.isError());
        assertTrue(Objects.requireNonNull(matchedInsert.errorMessage()).contains("cannot use INSERT"));
        assertTrue(notMatchedDelete.isError());
        assertTrue(Objects.requireNonNull(notMatchedDelete.errorMessage()).contains("must use INSERT or DO NOTHING"));
        assertTrue(bySourceInsert.isError());
        assertTrue(Objects.requireNonNull(bySourceInsert.errorMessage()).contains("cannot use INSERT"));
    }

    @Test
    void rejectsMalformedClausePredicate() {
        var ctx = ParseContext.of(new SupportedMergeClauseSpecs());
        var result = ctx.parse(new SupportedMergeClauseParser(), "WHEN MATCHED AND THEN DELETE");

        assertTrue(result.isError());
    }

    @Test
    void exposesMergeClauseTargetType() {
        assertEquals(MergeClause.class, new MergeClauseParser().targetType());
    }

    private static final class SupportedMergeClauseParser extends MergeClauseParser {
        @Override
        public ParseResult<? extends MergeClause> parse(Cursor cur, ParseContext ctx) {
            return parseSupportedClause(cur, ctx);
        }

        @Override
        protected MergeClause.MatchType parseNotMatchedBy(Cursor cur) {
            if (cur.consumeIf(TokenType.SOURCE)) {
                return MergeClause.MatchType.NOT_MATCHED_BY_SOURCE;
            }
            return null;
        }
    }

    private static final class UnsupportedByMergeClauseParser extends MergeClauseParser {
        @Override
        public ParseResult<? extends MergeClause> parse(Cursor cur, ParseContext ctx) {
            return parseSupportedClause(cur, ctx);
        }
    }

    private static final class SupportedMergeClauseSpecs implements Specs {
        private final TestSpecs delegate = new TestSpecs();
        private final ParsersRepository parsers = Parsers.ansiCopy()
            .register(new SupportedMergeDeleteActionParser())
            .register(new SupportedMergeUpdateActionParser())
            .register(new SupportedMergeInsertActionParser());

        @Override
        public ParsersRepository parsers() {
            return parsers;
        }

        @Override
        public Lookups lookups() {
            return delegate.lookups();
        }

        @Override
        public IdentifierQuoting identifierQuoting() {
            return delegate.identifierQuoting();
        }

        @Override
        public io.sqm.core.dialect.DialectCapabilities capabilities() {
            return delegate.capabilities();
        }

        @Override
        public OperatorPolicy operatorPolicy() {
            return delegate.operatorPolicy();
        }
    }

    private static final class SupportedMergeDeleteActionParser implements Parser<MergeDeleteAction> {
        @Override
        public ParseResult<? extends MergeDeleteAction> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected DELETE", TokenType.DELETE);
            return ParseResult.ok(MergeDeleteAction.of());
        }

        @Override
        public Class<MergeDeleteAction> targetType() {
            return MergeDeleteAction.class;
        }
    }

    private static final class SupportedMergeUpdateActionParser implements Parser<MergeUpdateAction> {
        @Override
        public ParseResult<? extends MergeUpdateAction> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected UPDATE", TokenType.UPDATE);
            cur.expect("Expected SET", TokenType.SET);
            var assignment = ctx.parse(io.sqm.core.Assignment.class, cur);
            if (assignment.isError()) {
                return ParseResult.error(assignment);
            }
            return ParseResult.ok(MergeUpdateAction.of(java.util.List.of(assignment.value())));
        }

        @Override
        public Class<MergeUpdateAction> targetType() {
            return MergeUpdateAction.class;
        }
    }

    private static final class SupportedMergeInsertActionParser implements Parser<MergeInsertAction> {
        @Override
        public ParseResult<? extends MergeInsertAction> parse(Cursor cur, ParseContext ctx) {
            cur.expect("Expected INSERT", TokenType.INSERT);
            var columns = java.util.List.<io.sqm.core.Identifier>of();
            if (cur.consumeIf(TokenType.LPAREN)) {
                columns = new java.util.ArrayList<>();
                columns.add(io.sqm.core.Identifier.of(cur.expect("Expected INSERT column", TokenType.IDENT).lexeme()));
                while (cur.consumeIf(TokenType.COMMA)) {
                    columns.add(io.sqm.core.Identifier.of(cur.expect("Expected INSERT column", TokenType.IDENT).lexeme()));
                }
                cur.expect("Expected ')' after INSERT column list", TokenType.RPAREN);
            }
            cur.expect("Expected VALUES", TokenType.VALUES);
            var values = ctx.parse(io.sqm.core.RowExpr.class, cur);
            if (values.isError()) {
                return ParseResult.error(values);
            }
            return ParseResult.ok(MergeInsertAction.of(columns, values.value()));
        }

        @Override
        public Class<MergeInsertAction> targetType() {
            return MergeInsertAction.class;
        }
    }
}
