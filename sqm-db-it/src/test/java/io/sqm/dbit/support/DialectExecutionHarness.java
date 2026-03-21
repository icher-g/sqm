package io.sqm.dbit.support;

import io.sqm.core.Statement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helper base for live-database execution suites.
 */
public abstract class DialectExecutionHarness {
    protected abstract Connection openConnection() throws Exception;

    protected abstract String render(Statement statement);

    /**
     * Executes one or more setup statements against the live database.
     *
     * @param statements SQL statements to execute in order
     * @throws Exception when setup execution fails
     */
    public void executeStatements(String... statements) throws Exception {
        try (var connection = openConnection(); var statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    /**
     * Executes a query without bind parameters and returns pipe-delimited rows.
     *
     * @param sql SQL query to execute
     * @return query rows encoded as pipe-delimited strings
     * @throws Exception when query execution fails
     */
    public List<String> queryRows(String sql) throws Exception {
        return queryRows(sql, List.of());
    }

    /**
     * Executes a query with bind parameters and returns pipe-delimited rows.
     *
     * @param sql    SQL query to execute
     * @param params bind parameter values
     * @return query rows encoded as pipe-delimited strings
     * @throws Exception when query execution fails
     */
    public List<String> queryRows(String sql, List<Object> params) throws Exception {
        List<String> rows = new ArrayList<>();
        try (var connection = openConnection(); var statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                int columns = resultSet.getMetaData().getColumnCount();
                while (resultSet.next()) {
                    List<String> values = new ArrayList<>(columns);
                    for (int index = 1; index <= columns; index++) {
                        values.add(stringify(resultSet.getObject(index)));
                    }
                    rows.add(String.join("|", values));
                }
            }
        }
        return rows;
    }

    /**
     * Executes a DML statement with bind parameters.
     *
     * @param sql    SQL statement to execute
     * @param params bind parameter values
     * @return affected row count
     * @throws Exception when statement execution fails
     */
    public int executeUpdate(String sql, List<Object> params) throws Exception {
        try (var connection = openConnection(); var statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, List<Object> params) throws Exception {
        for (int index = 0; index < params.size(); index++) {
            statement.setObject(index + 1, params.get(index));
        }
    }

    private String stringify(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return String.valueOf(value);
    }
}