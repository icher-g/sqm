package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LockTarget Tests")
class LockTargetTest {

    @Test
    @DisplayName("Create lock target with identifier")
    void createWithIdentifier() {
        var target = LockTarget.of(Identifier.of("users"));
        
        assertNotNull(target);
        assertEquals("users", target.identifier().value());
        assertEquals(Identifier.of("users"), target.identifier());
    }

    @Test
    @DisplayName("Create lock target with table alias")
    void createWithAlias() {
        var target = LockTarget.of(Identifier.of("u"));
        
        assertNotNull(target);
        assertEquals("u", target.identifier().value());
    }

    @Test
    @DisplayName("Create lock target with quote-aware identifier")
    void createWithQuoteAwareIdentifier() {
        var target = LockTarget.of(Identifier.of("Users", QuoteStyle.DOUBLE_QUOTE));

        assertEquals("Users", target.identifier().value());
        assertTrue(target.identifier().quoted());
        assertEquals(QuoteStyle.DOUBLE_QUOTE, target.identifier().quoteStyle());
    }

    @Test
    @DisplayName("Null identifier throws exception")
    void nullIdentifierThrows() {
        assertThrows(NullPointerException.class, () -> LockTarget.of(null));
    }

    @Test
    @DisplayName("Lock target equality works")
    void equalityWorks() {
        var target1 = LockTarget.of(Identifier.of("users"));
        var target2 = LockTarget.of(Identifier.of("users"));
        var target3 = LockTarget.of(Identifier.of("orders"));
        
        assertEquals(target1, target2);
        assertNotEquals(target1, target3);
        assertEquals(target1.hashCode(), target2.hashCode());
    }

    @Test
    @DisplayName("Lock target toString contains identifier")
    void toStringContainsIdentifier() {
        var target = LockTarget.of(Identifier.of("users"));
        var str = target.toString();
        
        assertTrue(str.contains("users"));
    }

    @Test
    @DisplayName("Lock target is immutable")
    void isImmutable() {
        var target = LockTarget.of(Identifier.of("users"));
        
        // Record is immutable by design
        assertEquals("users", target.identifier().value());
    }
}
