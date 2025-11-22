package io.sqm.it;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParametersTransformationTest {

    @Test
    void parsedOrdinalParameters_renderedAsQuestionMark() {
        var sql = "SELECT * FROM users WHERE status = $1 AND country = $2";
        var query = Utils.parse(sql);
        var rendered = Utils.renderAnsi(query);
        assertEquals("SELECT * FROM users WHERE status = ? AND country = ?", rendered);
    }

    @Test
    void parsedNamedParameters_renderedAsQuestionMark() {
        var sql = "SELECT * FROM users WHERE status = :n1 AND country = :n2";
        var query = Utils.parse(sql);
        var rendered = Utils.renderAnsi(query);
        assertEquals("SELECT * FROM users WHERE status = ? AND country = ?", rendered);
    }
}
