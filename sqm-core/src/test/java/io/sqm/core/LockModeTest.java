package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LockMode Tests")
class LockModeTest {

    @Test
    @DisplayName("All lock modes exist")
    void allLockModesExist() {
        assertEquals(4, LockMode.values().length);
        assertNotNull(LockMode.UPDATE);
        assertNotNull(LockMode.NO_KEY_UPDATE);
        assertNotNull(LockMode.SHARE);
        assertNotNull(LockMode.KEY_SHARE);
    }

    @Test
    @DisplayName("Lock mode valueOf works correctly")
    void valueOfWorks() {
        assertEquals(LockMode.UPDATE, LockMode.valueOf("UPDATE"));
        assertEquals(LockMode.NO_KEY_UPDATE, LockMode.valueOf("NO_KEY_UPDATE"));
        assertEquals(LockMode.SHARE, LockMode.valueOf("SHARE"));
        assertEquals(LockMode.KEY_SHARE, LockMode.valueOf("KEY_SHARE"));
    }

    @Test
    @DisplayName("Lock mode name() works correctly")
    void nameWorks() {
        assertEquals("UPDATE", LockMode.UPDATE.name());
        assertEquals("NO_KEY_UPDATE", LockMode.NO_KEY_UPDATE.name());
        assertEquals("SHARE", LockMode.SHARE.name());
        assertEquals("KEY_SHARE", LockMode.KEY_SHARE.name());
    }

    @Test
    @DisplayName("Lock mode ordinal values are consistent")
    void ordinalValues() {
        assertEquals(0, LockMode.UPDATE.ordinal());
        assertEquals(1, LockMode.NO_KEY_UPDATE.ordinal());
        assertEquals(2, LockMode.SHARE.ordinal());
        assertEquals(3, LockMode.KEY_SHARE.ordinal());
    }
}
