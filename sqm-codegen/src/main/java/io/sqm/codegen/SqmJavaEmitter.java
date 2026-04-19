package io.sqm.codegen;

import io.sqm.core.Statement;

final class SqmJavaEmitter {
    private final SqmDslVisitor visitor = new SqmDslVisitor();

    public String emit(Statement statement) {
        return visitor.emit(statement);
    }
}
