package io.sqm.parser.oracle;

import io.sqm.core.Node;
import io.sqm.core.Query;
import io.sqm.core.QueryExpr;
import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.*;

import java.util.Deque;

/**
 * Local MODEL context; entering a subquery restores the enclosing ordinary context.
 */
record ModelParseContext(ParseContext parent, ModelGrammar.Options options, ModelParseScope scope) implements ParseContext {
    @Override
    public ParsersRepository parsers() {
        return parent.parsers();
    }

    @Override
    public Lookups lookups() {
        return parent.lookups();
    }

    @Override
    public IdentifierQuoting identifierQuoting() {
        return parent.identifierQuoting();
    }

    @Override
    public DialectCapabilities capabilities() {
        return parent.capabilities();
    }

    @Override
    public OperatorPolicy operatorPolicy() {
        return parent.operatorPolicy();
    }

    @Override
    public Deque<Class<?>> callstack() {
        return parent.callstack();
    }

    @Override
    public <T extends Node> ParseResult<? extends T> parse(Parser<T> parser, Cursor cur) {
        if (Query.class.isAssignableFrom(parser.targetType()) || parser.targetType() == QueryExpr.class) {
            return parent.parse(parser, cur);
        }
        return ParseContext.super.parse(parser, cur);
    }
}
