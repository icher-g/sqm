package io.sqm.playground.rest.service;

import io.sqm.core.dialect.SqlDialectId;
import io.sqm.playground.api.SqlDialectDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaygroundDialectSupportTest {

    @Test
    void toDialectIdMapsAllPlaygroundDialects() {
        assertEquals(SqlDialectId.ANSI, PlaygroundDialectSupport.toDialectId(SqlDialectDto.ansi));
        assertEquals(SqlDialectId.POSTGRESQL, PlaygroundDialectSupport.toDialectId(SqlDialectDto.postgresql));
        assertEquals(SqlDialectId.MYSQL, PlaygroundDialectSupport.toDialectId(SqlDialectDto.mysql));
        assertEquals(SqlDialectId.SQLSERVER, PlaygroundDialectSupport.toDialectId(SqlDialectDto.sqlserver));
        assertEquals(SqlDialectId.ORACLE, PlaygroundDialectSupport.toDialectId(SqlDialectDto.oracle));
    }
}
