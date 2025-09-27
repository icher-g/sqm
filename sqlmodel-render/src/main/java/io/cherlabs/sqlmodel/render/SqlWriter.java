package io.cherlabs.sqlmodel.render;

import io.cherlabs.sqlmodel.core.Entity;

import java.util.List;

public interface SqlWriter {
    SqlWriter append(String s);

    <T extends Entity> SqlWriter append(T entity);

    void ignoreNewLine(boolean ignore);

    SqlWriter space();

    SqlWriter newline();

    SqlWriter indent();

    SqlWriter outdent();

    <T extends Entity> SqlWriter comma(List<T> parts);

    SqlText toText(List<Object> params);
}

