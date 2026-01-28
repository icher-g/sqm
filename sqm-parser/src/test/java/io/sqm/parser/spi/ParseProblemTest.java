package io.sqm.parser.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParseProblemTest {

    @Test
    void shouldCreateProblemWithMessageAndPosition() {
        var problem = new ParseProblem("syntax error", 42);
        
        assertEquals("syntax error", problem.message());
        assertEquals(42, problem.pos());
    }

    @Test
    void shouldHandleNegativePosition() {
        var problem = new ParseProblem("error", -1);
        
        assertEquals("error", problem.message());
        assertEquals(-1, problem.pos());
    }

    @Test
    void shouldHandleZeroPosition() {
        var problem = new ParseProblem("error at start", 0);
        
        assertEquals("error at start", problem.message());
        assertEquals(0, problem.pos());
    }

    @Test
    void shouldSupportRecordEquality() {
        var problem1 = new ParseProblem("error", 10);
        var problem2 = new ParseProblem("error", 10);
        var problem3 = new ParseProblem("error", 20);
        
        assertEquals(problem1, problem2);
        assertNotEquals(problem1, problem3);
    }

    @Test
    void shouldSupportRecordHashCode() {
        var problem1 = new ParseProblem("error", 10);
        var problem2 = new ParseProblem("error", 10);
        
        assertEquals(problem1.hashCode(), problem2.hashCode());
    }

    @Test
    void shouldSupportRecordToString() {
        var problem = new ParseProblem("test error", 5);
        
        String str = problem.toString();
        assertTrue(str.contains("test error"));
        assertTrue(str.contains("5"));
    }
}
