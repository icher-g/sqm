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
                "ansi-analytics-report",
                "ANSI Analytics Report",
                SqlDialectDto.ansi,
                """
                with regional_sales as (
                    select
                        c.region,
                        o.customer_id,
                        sum(o.total) as revenue,
                        count(*) as order_count
                    from orders o
                    join customer c on c.id = o.customer_id
                    where o.status in ('SHIPPED', 'DELIVERED')
                    group by c.region, o.customer_id
                )
                select
                    region,
                    customer_id,
                    revenue,
                    order_count
                from regional_sales
                where revenue > 1000
                order by revenue desc, customer_id asc
                offset 5 rows fetch next 10 rows only
                """
            ),
            new ExampleDto(
                "postgres-returning",
                "PostgreSQL RETURNING",
                SqlDialectDto.postgresql,
                "insert into customer (id, name) values (1, 'Alice') returning id, name"
            ),
            new ExampleDto(
                "postgres-merge-returning",
                "PostgreSQL MERGE RETURNING",
                SqlDialectDto.postgresql,
                """
                merge into customer as c
                using incoming_customer as s
                    on c.id = s.id
                when matched and s.active = true then
                    update set name = s.name, status = s.status
                when not matched and s.active = true then
                    insert (id, name, status)
                    values (s.id, s.name, s.status)
                returning c.id, c.name, c.status
                """
            ),
            new ExampleDto(
                "mysql-update-join",
                "MySQL UPDATE JOIN",
                SqlDialectDto.mysql,
                "update orders o join customer c on c.id = o.customer_id set o.status = 'priority' where c.vip = 1"
            ),
            new ExampleDto(
                "mysql-joined-update-hints",
                "MySQL Joined UPDATE With Hints",
                SqlDialectDto.mysql,
                """
                update /*+ BKA(o) */ orders as o use index (idx_orders_customer)
                inner join customer as c force index for join (idx_customer_region)
                    on c.id = o.customer_id
                set
                    o.status = 'priority',
                    o.review_flag = 'Y'
                where c.vip = 1
                  and c.region in ('EU', 'US')
                  and o.status <> 'shipped'
                """
            ),
            new ExampleDto(
                "sqlserver-top",
                "SQL Server TOP",
                SqlDialectDto.sqlserver,
                "select top 5 id, total from orders order by total desc"
            ),
            new ExampleDto(
                "sqlserver-merge-output",
                "SQL Server MERGE OUTPUT",
                SqlDialectDto.sqlserver,
                """
                merge top (10) into [orders] as [target]
                using [incoming_orders] as [src]
                    on [target].[id] = [src].[id]
                when matched and [src].[status] <> 'cancelled' then
                    update set [target].[status] = [src].[status], [target].[total] = [src].[total]
                when not matched then
                    insert ([id], [customer_id], [status], [total])
                    values ([src].[id], [src].[customer_id], [src].[status], [src].[total])
                output inserted.[id], inserted.[status], inserted.[total]
                """
            )
        );
    }
}
