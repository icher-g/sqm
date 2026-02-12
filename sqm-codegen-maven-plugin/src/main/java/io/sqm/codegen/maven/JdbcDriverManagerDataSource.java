package io.sqm.codegen.maven;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Minimal {@link DataSource} backed by {@link DriverManager}.
 * This keeps JDBC configuration local to the plugin without introducing pool dependencies.
 */
final class JdbcDriverManagerDataSource implements DataSource {
    private final String url;
    private final String username;
    private final String password;

    /**
     * Creates a DriverManager-backed data source.
     *
     * @param url JDBC URL.
     * @param username JDBC username, optional.
     * @param password JDBC password, optional.
     */
    JdbcDriverManagerDataSource(String url, String username, String password) {
        this.url = Objects.requireNonNull(url, "url");
        this.username = username;
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }
}

