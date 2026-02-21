package io.sqm.validate.policy;

import io.sqm.validate.api.ValidationProblem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadOnlySqlValidatorTest {
    private final ReadOnlySqlValidator validator = ReadOnlySqlValidator.of();

    @Test
    void accepts_select_statement() {
        var result = validator.validate("select id from users");
        assertTrue(result.ok());
    }

    @Test
    void accepts_with_select_statement() {
        var result = validator.validate("with u as (select id from users) select * from u");
        assertTrue(result.ok());
    }

    @Test
    void rejects_insert_statement() {
        var result = validator.validate("insert into users(id) values (1)");
        assertEquals(ValidationProblem.Code.DML_NOT_ALLOWED, result.problems().getFirst().code());
    }

    @Test
    void rejects_update_statement_with_leading_comments() {
        var result = validator.validate("/* agent sql */ -- note\r\n update users set name = 'x' where id = 1");
        assertEquals(ValidationProblem.Code.DML_NOT_ALLOWED, result.problems().getFirst().code());
    }

    @Test
    void rejects_create_statement() {
        var result = validator.validate("create table users_archive(id bigint)");
        assertEquals(ValidationProblem.Code.DDL_NOT_ALLOWED, result.problems().getFirst().code());
    }

    @Test
    void rejects_with_insert_statement() {
        var result = validator.validate("with moved as (delete from users where id = 1 returning id) insert into log select id from moved");
        assertEquals(ValidationProblem.Code.DML_NOT_ALLOWED, result.problems().getFirst().code());
    }

    @Test
    void ignores_keywords_inside_string_literals() {
        var result = validator.validate("select 'drop table users' as msg");
        assertTrue(result.ok());
    }

    @Test
    void ignores_keywords_inside_quoted_identifiers() {
        var result = validator.validate("select \"create\" from audit_log");
        assertTrue(result.ok());
    }
}
