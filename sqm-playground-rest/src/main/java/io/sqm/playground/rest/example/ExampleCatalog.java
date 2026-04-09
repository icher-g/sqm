package io.sqm.playground.rest.example;

import io.sqm.playground.api.ExampleDto;
import io.sqm.playground.api.SqlDialectDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * In-memory catalog of built-in SQL examples for the playground.
 */
@Component
public final class ExampleCatalog {

    /**
     * Creates the example catalog.
     */
    public ExampleCatalog() {
    }

    /**
     * Returns the built-in examples shown by the playground.
     *
     * @return ordered built-in examples
     */
    public List<ExampleDto> examples() {
        return List.of(
            new ExampleDto(
                "basic-select",
                "Basic SELECT",
                SqlDialectDto.ansi,
                "select id, name from customer"
            ),
            new ExampleDto(
                "postgres-returning",
                "PostgreSQL RETURNING",
                SqlDialectDto.postgresql,
                "insert into customer (id, name) values (1, 'Alice') returning id, name"
            ),
            new ExampleDto(
                "mysql-update-join",
                "MySQL UPDATE JOIN",
                SqlDialectDto.mysql,
                "update orders o join customer c on c.id = o.customer_id set o.status = 'priority' where c.vip = 1"
            ),
            new ExampleDto(
                "sqlserver-top",
                "SQL Server TOP",
                SqlDialectDto.sqlserver,
                "select top 5 id, total from orders order by total desc"
            )
        );
    }
}
