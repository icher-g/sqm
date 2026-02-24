package io.sqm.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests JSON serialization/deserialization for Predicate subtypes:
 * LikePredicate and RegexPredicate.
 */
public class PredicateSubtypesJsonTest {

    private final ObjectMapper mapper = SqmJsonMixins.createPretty();

    private <T> T roundTrip(T value, Class<T> type) throws Exception {
        String json = mapper.writeValueAsString(value);
        T back = mapper.readValue(json, type);
        assertEquals(value, back, "round-trip equality failed");
        return back;
    }

    private JsonNode toTree(Object value) throws Exception {
        return mapper.readTree(mapper.writeValueAsBytes(value));
    }

    /* ==================== LikePredicate Tests ==================== */

    @Test
    @DisplayName("LikePredicate: simple LIKE with pattern")
    void likePredicate_simple() throws Exception {
        var predicate = col("name").like("%John%");

        var back = roundTrip(predicate, LikePredicate.class);

        assertNotNull(back);
        assertEquals(LikeMode.LIKE, back.mode());
        assertFalse(back.negated());
        assertInstanceOf(ColumnExpr.class, back.value());
        assertEquals("name", back.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertInstanceOf(LiteralExpr.class, back.pattern());
        assertEquals("%John%", ((LiteralExpr) back.pattern()).value());
        assertNull(back.escape());

        JsonNode node = toTree(predicate);
        assertEquals("like", node.path("kind").asText());
    }

    @Test
    @DisplayName("LikePredicate: NOT LIKE")
    void likePredicate_notLike() throws Exception {
        var predicate = col("email").notLike("%@test.com");

        var back = roundTrip(predicate, LikePredicate.class);

        assertTrue(back.negated());
        assertEquals(LikeMode.LIKE, back.mode());
        assertEquals("email", back.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("%@test.com", ((LiteralExpr) back.pattern()).value());
    }

    @Test
    @DisplayName("LikePredicate: with ESCAPE clause")
    void likePredicate_withEscape() throws Exception {
        var predicate = col("path").like("%\\_%").escape("\\");

        var back = roundTrip(predicate, LikePredicate.class);

        assertNotNull(back.escape());
        assertInstanceOf(LiteralExpr.class, back.escape());
        assertEquals("\\", ((LiteralExpr) back.escape()).value());
        assertEquals("%\\_%", ((LiteralExpr) back.pattern()).value());
    }

    @Test
    @DisplayName("LikePredicate: ILIKE mode")
    void likePredicate_ilike() throws Exception {
        var predicate = col("name").ilike("%john%");

        var back = roundTrip(predicate, LikePredicate.class);

        assertEquals(LikeMode.ILIKE, back.mode());
        assertFalse(back.negated());
        assertEquals("%john%", ((LiteralExpr) back.pattern()).value());
    }

    @Test
    @DisplayName("LikePredicate: polymorphic deserialization as Predicate")
    void likePredicate_asPredicate() throws Exception {
        var predicate = col("description").like("%important%");

        String json = mapper.writeValueAsString(predicate);
        Predicate back = mapper.readValue(json, Predicate.class);

        assertInstanceOf(LikePredicate.class, back);
        assertEquals("%important%", ((LiteralExpr) ((LikePredicate) back).pattern()).value());
    }

    @Test
    @DisplayName("LikePredicate in WHERE clause")
    void likePredicate_inWhere() throws Exception {
        var query = select(col("*"))
            .from(tbl("users"))
            .where(col("name").like("%Smith%"))
            .build();

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.where());
        var predicate = back.where().<LikePredicate>matchPredicate().like(l -> l).orElse(null);
        assertNotNull(predicate);
        assertEquals("name", predicate.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("%Smith%", ((LiteralExpr) predicate.pattern()).value());
    }

    @Test
    @DisplayName("LikePredicate: pattern as expression (column)")
    void likePredicate_patternAsColumn() throws Exception {
        var predicate = LikePredicate.of(col("value"), col("pattern_col"), false);

        var back = roundTrip(predicate, LikePredicate.class);

        assertInstanceOf(ColumnExpr.class, back.pattern());
        assertEquals("pattern_col", back.pattern().matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    @DisplayName("LikePredicate: combined with AND")
    void likePredicate_combinedWithAnd() throws Exception {
        var query = select(col("*"))
            .from(tbl("products"))
            .where(
                col("name").like("%Phone%")
                    .and(col("description").like("%Apple%"))
            )
            .build();

        var back = roundTrip(query, SelectQuery.class);

        var andPredicate = back.where().<CompositePredicate>matchPredicate().and(a -> a).orElse(null);
        assertNotNull(andPredicate);

        var left = ((AndPredicate) andPredicate).lhs().<LikePredicate>matchPredicate().like(l -> l).orElse(null);
        var right = ((AndPredicate) andPredicate).rhs().<LikePredicate>matchPredicate().like(l -> l).orElse(null);

        assertNotNull(left);
        assertNotNull(right);
        assertEquals("name", left.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("description", right.value().matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    @DisplayName("LikePredicate: SIMILAR TO mode")
    void likePredicate_similarTo() throws Exception {
        var predicate = LikePredicate.of(LikeMode.SIMILAR_TO, col("code"), lit("\\d{3}-\\d{4}"), false);

        var back = roundTrip(predicate, LikePredicate.class);

        assertEquals(LikeMode.SIMILAR_TO, back.mode());
        assertEquals("code", back.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("\\d{3}-\\d{4}", ((LiteralExpr) back.pattern()).value());
    }

    /* ==================== RegexPredicate Tests ==================== */

    @Test
    @DisplayName("RegexPredicate: simple regex match")
    void regexPredicate_simple() throws Exception {
        var predicate = RegexPredicate.of(col("email"), lit("^[a-z]+@[a-z]+\\.[a-z]+$"), false);

        var back = roundTrip(predicate, RegexPredicate.class);

        assertNotNull(back);
        assertEquals(RegexMode.MATCH, back.mode());
        assertFalse(back.negated());
        assertInstanceOf(ColumnExpr.class, back.value());
        assertEquals("email", back.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertInstanceOf(LiteralExpr.class, back.pattern());
        assertEquals("^[a-z]+@[a-z]+\\.[a-z]+$", ((LiteralExpr) back.pattern()).value());

        JsonNode node = toTree(predicate);
        assertEquals("regex", node.path("kind").asText());
    }

    @Test
    @DisplayName("RegexPredicate: negated regex")
    void regexPredicate_negated() throws Exception {
        var predicate = RegexPredicate.of(col("name"), lit("^test.*"), true);

        var back = roundTrip(predicate, RegexPredicate.class);

        assertTrue(back.negated());
        assertEquals("^test.*", ((LiteralExpr) back.pattern()).value());
    }

    @Test
    @DisplayName("RegexPredicate: case insensitive mode")
    void regexPredicate_caseInsensitive() throws Exception {
        var predicate = RegexPredicate.of(RegexMode.MATCH_INSENSITIVE, col("title"), lit(".*important.*"), false);

        var back = roundTrip(predicate, RegexPredicate.class);

        assertEquals(RegexMode.MATCH_INSENSITIVE, back.mode());
        assertFalse(back.negated());
        assertEquals("title", back.value().matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    @DisplayName("RegexPredicate: polymorphic deserialization as Predicate")
    void regexPredicate_asPredicate() throws Exception {
        var predicate = RegexPredicate.of(col("phone"), lit("^\\d{3}-\\d{4}$"), false);

        String json = mapper.writeValueAsString(predicate);
        Predicate back = mapper.readValue(json, Predicate.class);

        assertInstanceOf(RegexPredicate.class, back);
        assertEquals("^\\d{3}-\\d{4}$", ((LiteralExpr) ((RegexPredicate) back).pattern()).value());
    }

    @Test
    @DisplayName("RegexPredicate in WHERE clause")
    void regexPredicate_inWhere() throws Exception {
        var query = select(col("*"))
            .from(tbl("logs"))
            .where(RegexPredicate.of(col("message"), lit(".*ERROR.*"), false))
            .build();

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.where());
        // Since RegexPredicate is not exposed through DSL WHERE chaining, check as UnaryPredicate
        var regexPredicate = back.where().<RegexPredicate>matchPredicate().regex(u -> u).orElse(null);
        assertNotNull(regexPredicate);
        assertInstanceOf(RegexPredicate.class, regexPredicate);
        assertEquals("message", regexPredicate.value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals(".*ERROR.*", ((LiteralExpr) regexPredicate.pattern()).value());
    }

    @Test
    @DisplayName("RegexPredicate: pattern as parameter")
    void regexPredicate_patternAsParam() throws Exception {
        var predicate = RegexPredicate.of(col("username"), ParamExpr.named("pattern"), false);

        var back = roundTrip(predicate, RegexPredicate.class);

        assertInstanceOf(NamedParamExpr.class, back.pattern());
        assertEquals("pattern", ((NamedParamExpr) back.pattern()).name());
    }

    @Test
    @DisplayName("RegexPredicate: all modes")
    void regexPredicate_allModes() throws Exception {
        var match = RegexPredicate.of(RegexMode.MATCH, col("text"), lit("pattern1"), false);
        var imatch = RegexPredicate.of(RegexMode.MATCH_INSENSITIVE, col("text"), lit("pattern2"), false);
        var notMatch = RegexPredicate.of(RegexMode.MATCH, col("text"), lit("pattern3"), true);
        var notImatch = RegexPredicate.of(RegexMode.MATCH_INSENSITIVE, col("text"), lit("pattern4"), true);

        var backMatch = roundTrip(match, RegexPredicate.class);
        var backImatch = roundTrip(imatch, RegexPredicate.class);
        var backNotMatch = roundTrip(notMatch, RegexPredicate.class);
        var backNotImatch = roundTrip(notImatch, RegexPredicate.class);

        assertFalse(backMatch.negated());
        assertEquals(RegexMode.MATCH, backMatch.mode());
        assertFalse(backImatch.negated());
        assertEquals(RegexMode.MATCH_INSENSITIVE, backImatch.mode());
        assertTrue(backNotMatch.negated());
        assertEquals(RegexMode.MATCH, backNotMatch.mode());
        assertTrue(backNotImatch.negated());
        assertEquals(RegexMode.MATCH_INSENSITIVE, backNotImatch.mode());
    }

    /* ==================== Integration Tests ==================== */

    @Test
    @DisplayName("Complex query with LIKE and REGEX predicates")
    void complex_likeAndRegex() throws Exception {
        var query = select(col("id"), col("name"), col("email"))
            .from(tbl("users"))
            .where(
                col("name").like("%Smith%")
                    .and(RegexPredicate.of(col("email"), lit("^[a-z]+@company\\.com$"), false))
                    .or(col("username").notLike("admin%"))
            )
            .build();

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.where());
        assertInstanceOf(CompositePredicate.class, back.where());

        JsonNode node = toTree(query);
        String whereJson = node.path("where").toString();
        assertTrue(whereJson.contains("like"));
        assertTrue(whereJson.contains("regex"));
    }

    @Test
    @DisplayName("LIKE with multiple patterns in OR")
    void like_multiplePatterns() throws Exception {
        var query = select(col("*"))
            .from(tbl("products"))
            .where(
                col("name").like("%Phone%")
                    .or(col("name").like("%Tablet%"))
                    .or(col("name").like("%Laptop%"))
            )
            .build();

        var back = roundTrip(query, SelectQuery.class);

        var orPredicate = back.where().<CompositePredicate>matchPredicate().or(o -> o).orElse(null);
        assertNotNull(orPredicate);

        JsonNode node = toTree(query);
        String whereJson = node.path("where").toString();
        assertTrue(whereJson.contains("%Phone%"));
        assertTrue(whereJson.contains("%Tablet%"));
        assertTrue(whereJson.contains("%Laptop%"));
    }

    @Test
    @DisplayName("JSON format verification for predicates")
    void jsonFormat_predicates() throws Exception {
        var like = col("name").like("%test%");
        var regex = RegexPredicate.of(col("email"), lit("^test.*"), false);

        String likeJson = mapper.writeValueAsString(like);
        assertTrue(likeJson.contains("\"kind\" : \"like\""));
        assertTrue(likeJson.contains("\"mode\" : \"LIKE\""));
        assertTrue(likeJson.contains("\"negated\" : false"));

        String regexJson = mapper.writeValueAsString(regex);
        assertTrue(regexJson.contains("\"kind\" : \"regex\""));
        assertTrue(regexJson.contains("\"mode\" : \"MATCH\""));
        assertTrue(regexJson.contains("\"negated\" : false"));
    }

    @Test
    @DisplayName("LIKE with escape in complex query")
    void like_escapeInComplexQuery() throws Exception {
        var query = select(col("id"), col("filename"))
            .from(tbl("files"))
            .where(
                col("filename").like("%\\_archive\\_%").escape("\\")
                    .and(col("status").eq(lit("active")))
            )
            .build();

        var back = roundTrip(query, SelectQuery.class);

        var andPred = back.where().<CompositePredicate>matchPredicate().and(a -> a).orElse(null);
        assertNotNull(andPred);

        var likePred = ((AndPredicate) andPred).lhs().<LikePredicate>matchPredicate().like(l -> l).orElse(null);
        assertNotNull(likePred);
        assertNotNull(likePred.escape());
        assertEquals("\\", ((LiteralExpr) likePred.escape()).value());
    }

    @Test
    @DisplayName("Regex with all expression types")
    void regex_variousExpressionTypes() throws Exception {
        // Test regex with column, literal, and parameter as value
        var regexWithCol = RegexPredicate.of(col("field"), lit("pattern"), false);
        var regexWithFunc = RegexPredicate.of(
            func("lower", arg(col("field"))),
            lit("pattern"),
            false
        );

        var backCol = roundTrip(regexWithCol, RegexPredicate.class);
        var backFunc = roundTrip(regexWithFunc, RegexPredicate.class);

        assertInstanceOf(ColumnExpr.class, backCol.value());
        assertInstanceOf(FunctionExpr.class, backFunc.value());
    }

    @Test
    @DisplayName("LIKE and REGEX combined with NOT")
    void predicates_withNot() throws Exception {
        var query = select(col("*"))
            .from(tbl("records"))
            .where(
                col("name").like("%test%").not()
                    .and(RegexPredicate.of(col("code"), lit("^[A-Z]{3}$"), true))
            )
            .build();

        var back = roundTrip(query, SelectQuery.class);

        assertNotNull(back.where());

        JsonNode node = toTree(query);
        String whereJson = node.path("where").toString();
        assertTrue(whereJson.contains("\"kind\":\"not\""));
        assertTrue(whereJson.contains("\"kind\":\"regex\""));
        assertTrue(whereJson.contains("\"negated\":true"));
    }

    @Test
    @DisplayName("LIKE predicate with all LikeMode values")
    void like_allModes() throws Exception {
        var like = LikePredicate.of(LikeMode.LIKE, col("name"), lit("%test%"), false);
        var ilike = LikePredicate.of(LikeMode.ILIKE, col("name"), lit("%test%"), false);
        var similar = LikePredicate.of(LikeMode.SIMILAR_TO, col("name"), lit("test.*"), false);

        var backLike = roundTrip(like, LikePredicate.class);
        var backIlike = roundTrip(ilike, LikePredicate.class);
        var backSimilar = roundTrip(similar, LikePredicate.class);

        assertEquals(LikeMode.LIKE, backLike.mode());
        assertEquals(LikeMode.ILIKE, backIlike.mode());
        assertEquals(LikeMode.SIMILAR_TO, backSimilar.mode());
    }
}
