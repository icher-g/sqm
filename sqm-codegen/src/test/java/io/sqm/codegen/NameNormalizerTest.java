package io.sqm.codegen;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameNormalizerTest {

    @Test
    void toClassName_forRootFolder_returnsRootQueries() {
        assertEquals("RootQueries", NameNormalizer.toClassName(Path.of("")));
    }

    @Test
    void toClassName_forNestedFolders_returnsPascalCaseQueriesSuffix() {
        assertEquals("ReportingDailyQueries", NameNormalizer.toClassName(Path.of("reporting", "daily")));
    }

    @Test
    void toMethodName_forSnakeCase_returnsCamelCase() {
        assertEquals("findById", NameNormalizer.toMethodName("find_by_id"));
    }

    @Test
    void toMethodName_forKebabCase_returnsCamelCase() {
        assertEquals("listActive", NameNormalizer.toMethodName("list-active"));
    }

    @Test
    void toMethodName_forNumericPrefix_addsIdentifierPrefix() {
        assertEquals("q123Query", NameNormalizer.toMethodName("123_query"));
    }

    @Test
    void toClassName_forNumericLeadingSegment_addsClassPrefix() {
        assertEquals("Q123DailyQueries", NameNormalizer.toClassName(Path.of("123", "daily")));
    }

    @Test
    void toClassName_forOnlySeparators_returnsRootQueries() {
        assertEquals("RootQueries", NameNormalizer.toClassName(Path.of("__")));
    }

    @Test
    void toMethodName_forOnlySeparators_returnsDefaultQueryName() {
        assertEquals("query", NameNormalizer.toMethodName("__"));
    }
}
