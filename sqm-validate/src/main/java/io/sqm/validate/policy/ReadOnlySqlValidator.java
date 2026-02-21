package io.sqm.validate.policy;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.validate.api.SqlValidator;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.api.ValidationResult;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * SQL text validator that rejects write/definition statements in read-only mode.
 */
public final class ReadOnlySqlValidator implements SqlValidator {
    private static final IdentifierQuoting DEFAULT_QUOTING = IdentifierQuoting.of('"', '`', '[');

    private static final EnumSet<TokenType> DML_TOKEN_TYPES = EnumSet.of(
        TokenType.INSERT, TokenType.UPDATE, TokenType.DELETE, TokenType.MERGE,
        TokenType.TRUNCATE, TokenType.REPLACE, TokenType.COPY
    );
    private static final EnumSet<TokenType> DDL_TOKEN_TYPES = EnumSet.of(
        TokenType.CREATE, TokenType.ALTER, TokenType.DROP, TokenType.GRANT,
        TokenType.REVOKE, TokenType.COMMENT, TokenType.RENAME
    );
    private static final EnumSet<TokenType> STATEMENT_START_TOKEN_TYPES = EnumSet.of(
        TokenType.SELECT, TokenType.INSERT, TokenType.UPDATE, TokenType.DELETE, TokenType.MERGE,
        TokenType.TRUNCATE, TokenType.REPLACE, TokenType.COPY, TokenType.CREATE, TokenType.ALTER,
        TokenType.DROP, TokenType.GRANT, TokenType.REVOKE, TokenType.COMMENT, TokenType.RENAME
    );

    private ReadOnlySqlValidator() {
    }

    /**
     * Creates a read-only SQL validator.
     *
     * @return validator instance
     */
    public static ReadOnlySqlValidator of() {
        return new ReadOnlySqlValidator();
    }

    /**
     * Validates SQL text and reports DDL/DML usage when present.
     *
     * @param sql SQL text.
     * @return validation result.
     */
    @Override
    public ValidationResult validate(String sql) {
        Objects.requireNonNull(sql, "sql");
        var token = firstDisallowedTopLevelStatementToken(sql);
        if (token == null) {
            return new ValidationResult(List.of());
        }
        if (DML_TOKEN_TYPES.contains(token.type())) {
            return new ValidationResult(List.of(
                new ValidationProblem(
                    ValidationProblem.Code.DML_NOT_ALLOWED,
                    "DML statement is not allowed in read-only mode: " + token.lexeme().toUpperCase(Locale.ROOT),
                    "SqlStatement",
                    "statement"
                )
            ));
        }
        if (DDL_TOKEN_TYPES.contains(token.type())) {
            return new ValidationResult(List.of(
                new ValidationProblem(
                    ValidationProblem.Code.DDL_NOT_ALLOWED,
                    "DDL statement is not allowed in read-only mode: " + token.lexeme().toUpperCase(Locale.ROOT),
                    "SqlStatement",
                    "statement"
                )
            ));
        }
        return new ValidationResult(List.of());
    }

    private static Token firstDisallowedTopLevelStatementToken(String sql) {
        var cursor = Cursor.of(sql, DEFAULT_QUOTING);
        int lookahead = 0;
        while (true) {
            int i = cursor.find(STATEMENT_START_TOKEN_TYPES, lookahead);
            if (i >= cursor.size()) {
                return null;
            }
            var token = cursor.peek(i - cursor.pos());
            if (DML_TOKEN_TYPES.contains(token.type()) || DDL_TOKEN_TYPES.contains(token.type())) {
                return token;
            }
            lookahead = (i - cursor.pos()) + 1;
        }
    }
}
