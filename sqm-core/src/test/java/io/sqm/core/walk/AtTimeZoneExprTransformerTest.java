package io.sqm.core.walk;

import io.sqm.core.*;
import io.sqm.core.transform.RecursiveNodeTransformer;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AtTimeZoneExpr} transformer support.
 * <p>
 * These tests verify that:
 * <ul>
 *     <li>{@link RecursiveNodeTransformer} correctly transforms AT TIME ZONE expressions</li>
 *     <li>Child expressions (timestamp and timezone) are recursively transformed</li>
 *     <li>New transformed instances are created rather than modifying existing ones</li>
 * </ul>
 */
class AtTimeZoneExprTransformerTest {

    @Test
    void transform_preservesUnchangedAtTimeZoneExpr() {
        AtTimeZoneExpr expr = AtTimeZoneExpr.of(col("ts"), lit("UTC"));

        Node transformed = expr.accept(new RecursiveNodeTransformer() {});

        assertNotNull(transformed);
        assertInstanceOf(AtTimeZoneExpr.class, transformed);
        AtTimeZoneExpr result = (AtTimeZoneExpr) transformed;
        
        assertInstanceOf(ColumnExpr.class, result.timestamp());
        assertInstanceOf(LiteralExpr.class, result.timezone());
    }

    @Test
    void transform_replacesTimestampColumn() {
        AtTimeZoneExpr expr = AtTimeZoneExpr.of(col("old_ts"), lit("UTC"));

        RenameColumnTransformer transformer = new RenameColumnTransformer("old_ts", "new_ts");
        Node transformed = expr.accept(transformer);

        assertInstanceOf(AtTimeZoneExpr.class, transformed);
        AtTimeZoneExpr result = (AtTimeZoneExpr) transformed;
        ColumnExpr timestamp = (ColumnExpr) result.timestamp();
        assertEquals("new_ts", timestamp.name().value());
    }

    @Test
    void transform_replacesTimezoneExpression() {
        AtTimeZoneExpr expr = AtTimeZoneExpr.of(col("ts"), lit("UTC"));

        TimezoneReplacer transformer = new TimezoneReplacer();
        Node transformed = expr.accept(transformer);

        assertInstanceOf(AtTimeZoneExpr.class, transformed);
        AtTimeZoneExpr result = (AtTimeZoneExpr) transformed;
        ColumnExpr timezone = (ColumnExpr) result.timezone();
        assertEquals("tz_name", timezone.name().value());
    }

    @Test
    void transform_createsNewInstance() {
        AtTimeZoneExpr original = AtTimeZoneExpr.of(col("ts"), lit("UTC"));

        // Rename transformer that WILL match and create a new instance
        RenameColumnTransformer transformer = new RenameColumnTransformer("ts", "new_ts");
        Node transformed = original.accept(transformer);

        // Since the transformer modifies the timestamp column, a new instance should be created
        assertNotSame(original, transformed);
        assertInstanceOf(AtTimeZoneExpr.class, transformed);
    }

    @Test
    void transform_withNestedAtTimeZone() {
        // Inner: ts AT TIME ZONE 'UTC'
        AtTimeZoneExpr inner = AtTimeZoneExpr.of(col("ts"), lit("UTC"));
        // Outer: (ts AT TIME ZONE 'UTC') AT TIME ZONE tz_col
        AtTimeZoneExpr outer = AtTimeZoneExpr.of(inner, col("user_tz"));

        RenameColumnTransformer transformer = new RenameColumnTransformer("ts", "event_ts");
        Node transformed = outer.accept(transformer);

        assertInstanceOf(AtTimeZoneExpr.class, transformed);
        AtTimeZoneExpr resultOuter = (AtTimeZoneExpr) transformed;
        assertInstanceOf(AtTimeZoneExpr.class, resultOuter.timestamp());

        AtTimeZoneExpr resultInner = (AtTimeZoneExpr) resultOuter.timestamp();
        ColumnExpr innerTimestamp = (ColumnExpr) resultInner.timestamp();
        assertEquals("event_ts", innerTimestamp.name().value());
    }

    /**
     * Transformer that renames a specific column.
     */
    private static class RenameColumnTransformer extends RecursiveNodeTransformer {
        private final String oldName;
        private final String newName;

        RenameColumnTransformer(String oldName, String newName) {
            this.oldName = oldName;
            this.newName = newName;
        }

        @Override
        public Node visitColumnExpr(ColumnExpr c) {
            if (oldName.equals(c.name().value())) {
                return ColumnExpr.of(null, Identifier.of(newName)).inTable(c.tableAlias());
            }
            return c;
        }
    }

    /**
     * Transformer that replaces all literal timezone expressions with a column reference.
     */
    private static class TimezoneReplacer extends RecursiveNodeTransformer {
        @Override
        public Node visitLiteralExpr(LiteralExpr l) {
            // For testing purposes: replace literals used in AT TIME ZONE with column reference
            return ColumnExpr.of(null, Identifier.of("tz_name"));
        }
    }
}
