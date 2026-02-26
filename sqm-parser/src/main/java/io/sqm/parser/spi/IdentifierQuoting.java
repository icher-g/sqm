package io.sqm.parser.spi;

import java.util.Set;

/**
 * Defines rules for quoting (delimiting) SQL identifiers.
 * <p>
 * Some SQL dialects allow identifiers (such as table or column names)
 * to be wrapped in specific quoting characters to preserve case,
 * allow reserved keywords, or include special characters.
 * Common examples include:
 * <ul>
 *   <li>{@code "identifier"} (ANSI SQL, PostgreSQL)</li>
 *   <li>{@code [identifier]} (SQL Server)</li>
 *   <li>{@code `identifier`} (MySQL, MariaDB)</li>
 * </ul>
 * <p>
 * This interface is typically used by the lexer to detect and parse
 * quoted identifiers in a dialect-specific manner.
 */
public interface IdentifierQuoting {

    /**
     * Creates an instance of {@link IdentifierQuoting} with the provided character(s) to be used as identifier(s).
     *
     * @param characters a list or a single character to be used as identifier(s).
     * @return a new instance of {@link IdentifierQuoting}.
     */
    static IdentifierQuoting of(Character... characters) {
        return new Impl(Set.of(characters));
    }

    /**
     * Checks whether the given character can start a quoted identifier.
     *
     * @param ch a character from the input stream.
     * @return {@code true} if the character is a supported opening
     * quoting character, {@code false} otherwise.
     */
    boolean supports(char ch);

    /**
     * Default immutable identifier-quoting implementation.
     *
     * @param characters supported opening quote characters
     */
    record Impl(Set<Character> characters) implements IdentifierQuoting {

        /**
         * Checks whether the given character can start a quoted identifier.
         *
         * @param ch a character from the input stream.
         * @return {@code true} if the character is a supported opening
         * quoting character, {@code false} otherwise.
         */
        @Override
        public boolean supports(char ch) {
            return characters.contains(ch);
        }
    }
}
