package io.sqm.codegen.maven;

/**
 * Resolved JDBC credentials used by schema introspection.
 *
 * @param username JDBC username, optional.
 * @param password JDBC password, optional.
 */
record JdbcCredentials(String username, String password) {
}

