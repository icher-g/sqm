package io.sqm.schema.introspect.jdbc;

import io.sqm.validate.schema.model.DbType;

/**
 * Maps JDBC/native SQL type metadata to SQM validation types.
 */
public interface SqlTypeMapper {
    /**
     * Maps database type metadata to a semantic SQM type.
     *
     * @param nativeTypeName database-specific type name, may be null.
     * @param jdbcType JDBC type code from {@link java.sql.Types}.
     * @return mapped SQM type.
     */
    DbType map(String nativeTypeName, int jdbcType);
}

