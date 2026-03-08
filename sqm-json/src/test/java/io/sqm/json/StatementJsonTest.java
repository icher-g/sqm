package io.sqm.json;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.delete;
import static io.sqm.dsl.Dsl.insert;
import static io.sqm.dsl.Dsl.lit;
import static io.sqm.dsl.Dsl.row;
import static io.sqm.dsl.Dsl.select;
import static io.sqm.dsl.Dsl.set;
import static io.sqm.dsl.Dsl.tbl;
import static io.sqm.dsl.Dsl.update;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
}
