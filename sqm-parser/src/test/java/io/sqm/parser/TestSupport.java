package io.sqm.parser;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Lookahead;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.*;

import static io.sqm.parser.core.OperatorTokens.*;

final class TestSupport {

    private TestSupport() {
    }

    static ParseContext context(ParsersRepository parsers) {
        return ParseContext.of(new TestSpecs(parsers));
    }

    private static final class TestSpecs implements Specs {
        private final ParsersRepository parsers;
        private final Lookups lookups = new NoopLookups();
        private final IdentifierQuoting quoting = IdentifierQuoting.of('"');
        private final DialectCapabilities capabilities = VersionedDialectCapabilities.builder(SqlDialectVersion.minimum())
            .supports(SqlDialectVersion.minimum(), SqlFeature.values())
            .build();

        private TestSpecs(ParsersRepository parsers) {
            this.parsers = parsers;
        }

        @Override
        public ParsersRepository parsers() {
            return parsers;
        }

        @Override
        public Lookups lookups() {
            return lookups;
        }

        @Override
        public IdentifierQuoting identifierQuoting() {
            return quoting;
        }

        @Override
        public DialectCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public OperatorPolicy operatorPolicy() {
            return t -> (t.type() == TokenType.OPERATOR || t.type() == TokenType.QMARK)
                && !isArithmetic(t) && !isComparison(t) && !isRegex(t);
        }
    }

    private static final class NoopLookups implements Lookups {
        @Override
        public boolean looksLikeExpression(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeCaseExpr(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeColumnRef(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeFunctionCall(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikePredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeUnaryPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeAnyAllPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeBetweenPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeComparisonPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeExistsPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeInPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeIsNullPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeLikePredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeNotPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeAndPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeOrPredicate(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeValueSet(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeQueryExpr(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeRowExpr(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeRowListExpr(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeLiteralExpr(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeSelectItem(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeStar(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeQualifiedStar(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeQuery(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeSelectQuery(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeWithQuery(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeCompositeQuery(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeTableRef(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeQueryTable(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeValuesTable(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeTable(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeJoin(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeCrossJoin(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeNaturalJoin(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeUsingJoin(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeOnJoin(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeParam(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeAnonymousParam(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeNamedParam(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeOrdinalParam(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeArithmeticOperation(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeAdd(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeSub(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeMul(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeDiv(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeMod(Cursor cur, Lookahead pos) {
            return false;
        }

        @Override
        public boolean looksLikeNeg(Cursor cur, Lookahead pos) {
            return false;
        }
    }
}
