package io.sqm.codegen;

import io.sqm.core.Statement;

import java.util.Set;

record GeneratedStatementMethod(String methodName, Statement statement, Set<String> parameters) {
}
