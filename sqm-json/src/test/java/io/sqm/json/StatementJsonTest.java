package io.sqm.json;

import io.sqm.core.DeleteStatement;
import io.sqm.core.OutputRowSource;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatementJsonTest {

    @Test
    void deserializesQueryStatementRoot() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var json = mapper.writeValueAsString(select(lit(1)).build());

        var statement = mapper.readValue(json, Statement.class);

        assertInstanceOf(io.sqm.core.SelectQuery.class, statement);
    }

    @Test
    void deserializesInsertStatementRoot() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var json = mapper.writeValueAsString(insert(tbl("users"))
            .values(row(lit(1)))
            .build());

        var statement = mapper.readValue(json, Statement.class);

        assertInstanceOf(io.sqm.core.InsertStatement.class, statement);
    }

    @Test
    void deserializesUpdateStatementRoot() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var json = mapper.writeValueAsString(update(tbl("users"))
            .set(set("name", lit("alice")))
            .build());

        var statement = mapper.readValue(json, Statement.class);

        assertInstanceOf(UpdateStatement.class, statement);
    }

    @Test
    void deserializesDeleteStatementRoot() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var json = mapper.writeValueAsString(delete(tbl("users"))
            .where(col("id").eq(lit(1)))
            .build());

        var statement = mapper.readValue(json, Statement.class);

        assertInstanceOf(DeleteStatement.class, statement);
    }

    @Test
    void deserializesMergeStatementRoot() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var json = mapper.writeValueAsString(merge(tbl("users"))
            .source(tbl("src").as("s"))
            .on(col("users", "id").eq(col("s", "id")))
            .whenMatchedUpdate(java.util.List.of(set("name", col("s", "name"))))
            .whenNotMatchedInsert(java.util.List.of(id("id"), id("name")), row(col("s", "id"), col("s", "name")))
            .build());

        var statement = mapper.readValue(json, Statement.class);

        assertInstanceOf(io.sqm.core.MergeStatement.class, statement);
    }

    @Test
    void deserializesStatementWithOutputClause() throws Exception {
        var mapper = SqmJsonMixins.createDefault();
        var json = mapper.writeValueAsString(update(tbl("users"))
            .set(set("name", lit("alice")))
            .result(resultInto(tbl("audit"), "user_id"), insertedAll(), inserted("id").as("user_id"))
            .build());

        var statement = (UpdateStatement) mapper.readValue(json, Statement.class);

        assertInstanceOf(UpdateStatement.class, statement);
        assertNotNull(statement.result());
        assertInstanceOf(io.sqm.core.OutputStarResultItem.class, statement.result().items().getFirst());
        assertInstanceOf(io.sqm.core.ExprResultItem.class, statement.result().items().get(1));
        var outputStar = (io.sqm.core.OutputStarResultItem) statement.result().items().getFirst();
        assertInstanceOf(io.sqm.core.OutputStarResultItem.class, outputStar);
        org.junit.jupiter.api.Assertions.assertEquals(OutputRowSource.INSERTED, outputStar.source());
    }
}
