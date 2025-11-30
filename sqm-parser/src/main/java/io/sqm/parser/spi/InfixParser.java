package io.sqm.parser.spi;

import io.sqm.core.Node;
import io.sqm.parser.core.Cursor;

/**
 * An interface to parse a binary expression when the left-handed operand is already available and {@link Cursor} is placed on the operator.
 *
 * @param <O> The type of the left-hand operand.
 * @param <T> The type of the returned node.
 */
public interface InfixParser<O extends Node, T extends Node> {
    /**
     * Parses a binary operator occurrence where the left-hand side operand
     * has already been parsed.
     *
     * <p>The cursor is positioned at the operator token when this method
     * is invoked. Implementations are responsible for consuming the operator
     * token, parsing the right-hand side operand, and constructing the
     * resulting node.</p>
     *
     * @param lhs the already parsed left-hand operand
     * @param cur the cursor positioned at the operator token
     * @param ctx the parse context
     * @return the parsing result representing {@code lhs <op> rhs}
     */
    ParseResult<T> parse(O lhs, Cursor cur, ParseContext ctx);
}

