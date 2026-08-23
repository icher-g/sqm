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
                "SELECT id, name FROM customer"
            ),
            new ExampleDto(
                "ansi-analytics-report",
                "ANSI Analytics Report",
                SqlDialectDto.ansi,
                """
                WITH regional_sales AS (
                    SELECT
                        c.region,
                        o.customer_id,
                        SUM(o.total) AS revenue,
                        COUNT(*) AS order_count
                    FROM orders o
                    JOIN customer c ON c.id = o.customer_id
                    WHERE o.status IN ('SHIPPED', 'DELIVERED')
                    GROUP BY c.region, o.customer_id
                )
                SELECT
                    region,
                    customer_id,
                    revenue,
                    order_count
                FROM regional_sales
                WHERE revenue > 1000
                ORDER BY revenue DESC, customer_id ASC
                OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY
                """
            ),
            new ExampleDto(
                "postgres-returning",
                "PostgreSQL RETURNING",
                SqlDialectDto.postgresql,
                "INSERT INTO customer (id, name) VALUES (1, 'Alice') RETURNING id, name"
            ),
            new ExampleDto(
                "postgres-merge-returning",
                "PostgreSQL MERGE RETURNING",
                SqlDialectDto.postgresql,
                """
                MERGE INTO customer AS c
                USING incoming_customer AS s ON c.id = s.id
                WHEN MATCHED AND s.active = true THEN
                    UPDATE set name = s.name, status = s.status
                WHEN NOT MATCHED AND s.active = true THEN
                    INSERT (id, name, status)
                    VALUES (s.id, s.name, s.status)
                RETURNING c.id, c.name, c.status
                """
            ),
            new ExampleDto(
                "mysql-update-join",
                "MySQL UPDATE JOIN",
                SqlDialectDto.mysql,
                "UPDATE orders o JOIN customer c ON c.id = o.customer_id SET o.status = 'priority' WHERE c.vip = 1"
            ),
            new ExampleDto(
                "mysql-joined-update-hints",
                "MySQL Joined UPDATE With Hints",
                SqlDialectDto.mysql,
                """
                UPDATE /*+ BKA(o) */ orders AS o USE INDEX (idx_orders_customer)
                INNER JOIN customer AS c FORCE INDEX FOR JOIN (idx_customer_region)
                    ON c.id = o.customer_id
                SET
                    o.status = 'priority',
                    o.review_flag = 'Y'
                WHERE c.vip = 1
                  AND c.region IN ('EU', 'US')
                  AND o.status <> 'shipped'
                """
            ),
            new ExampleDto(
                "sqlserver-top",
                "SQL Server TOP",
                SqlDialectDto.sqlserver,
                "SELECT TOP 5 id, total FROM orders ORDER BY total DESC"
            ),
            new ExampleDto(
                "sqlserver-merge-output",
                "SQL Server MERGE OUTPUT",
                SqlDialectDto.sqlserver,
                """
                MERGE TOP (10) INTO [orders] AS [target]
                USING [incoming_orders] AS [src]
                    ON [target].[id] = [src].[id]
                WHEN MATCHED AND [src].[status] <> 'cancelled' THEN
                    UPDATE SET [target].[status] = [src].[status], [target].[total] = [src].[total]
                WHEN NOT MATCHED THEN
                    INSERT ([id], [customer_id], [status], [total])
                    VALUES ([src].[id], [src].[customer_id], [src].[status], [src].[total])
                OUTPUT inserted.[id], inserted.[status], inserted.[total]
                """
            ),
            new ExampleDto(
                "oracle-offset-fetch",
                "Oracle OFFSET FETCH",
                SqlDialectDto.oracle,
                """
                SELECT
                    c.id,
                    c.name
                FROM customer c
                ORDER BY c.id
                OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY
                """
            ),
            new ExampleDto(
                "oracle-hierarchical-query",
                "Oracle Hierarchical Query",
                SqlDialectDto.oracle,
                """
                SELECT
                    id,
                    parent_id,
                    level
                FROM categories
                START WITH parent_id IS null
                CONNECT BY PRIOR id = parent_id
                """
            ),
            new ExampleDto(
                "oracle-pivot-query",
                "Oracle Pivot Query",
                SqlDialectDto.oracle,
                """
                SELECT p.region, p.q1, m.manager_name
                FROM sales
                PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1)) p
                JOIN managers m ON m.region = p.region
                WHERE m.active = 1
                ORDER BY p.region
                """
            ),
            new ExampleDto(
                "oracle-match-recognize",
                "Oracle MATCH_RECOGNIZE",
                SqlDialectDto.oracle,
                """
                SELECT *
                FROM sales MATCH_RECOGNIZE (
                    PARTITION BY customer_id
                    ORDER BY sale_date
                    MEASURES
                        MATCH_NUMBER() AS match_no,
                        FIRST(A.amount) AS first_amount,
                        LAST(B.amount) AS last_amount
                    ONE ROW PER MATCH
                    AFTER MATCH SKIP TO NEXT ROW
                    PATTERN (A B+)
                    DEFINE
                        A AS A.amount > 0,
                        B AS B.amount > PREV(B.amount)
                ) mr
                """
            )
        );
    }
}
