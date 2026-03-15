package io.sqm.render.sqlserver.spi;

import io.sqm.render.spi.Booleans;

/**
 * SQL Server boolean literal behavior.
 */
public class SqlServerBooleans implements Booleans {

    /**
     * Creates SQL Server boolean behavior definition.
     */
    public SqlServerBooleans() {
    }

    @Override
    public String trueLiteral() {
        return "1";
    }

    @Override
    public String falseLiteral() {
        return "0";
    }

    @Override
    public boolean requireExplicitPredicate() {
        return true;
    }
}
