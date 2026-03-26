package io.sqm.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqm.core.Expression;
import io.sqm.core.HintArg;
import io.sqm.core.IdentifierHintArg;
import io.sqm.core.StatementHint;
import io.sqm.core.Table;
import io.sqm.core.TableHint;
import io.sqm.core.TableRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HintJsonTest {

    private final ObjectMapper mapper = SqmJsonMixins.createDefault();

    @Test
    void serializesAndDeserializesGenericStatementHint() throws Exception {
        var hint = StatementHint.of(
            "MAX_EXECUTION_TIME",
            HintArg.expression(Expression.literal(1000)),
            HintArg.identifier("FAST")
        );

        var json = mapper.writeValueAsString(hint);
        var restored = mapper.readValue(json, io.sqm.core.Hint.class);

        assertTrue(json.contains("\"kind\":\"statement_hint\""));
        var restoredHint = assertInstanceOf(StatementHint.class, restored);
        assertEquals("MAX_EXECUTION_TIME", restoredHint.name().value());
        assertEquals(2, restoredHint.args().size());
    }

    @Test
    void serializesTableHintsThroughTableReferenceMixin() throws Exception {
        var table = TableRef.table(io.sqm.core.Identifier.of("users"))
            .withNoLock()
            .useIndex("idx_users_name");

        var json = mapper.writeValueAsString(table);
        var restored = mapper.readValue(json, io.sqm.core.TableRef.class);

        assertTrue(json.contains("\"kind\":\"table_hint\""));

        var restoredTable = assertInstanceOf(Table.class, restored);
        assertEquals(2, restoredTable.hints().size());
        var indexHint = assertInstanceOf(TableHint.class, restoredTable.hints().getLast());
        assertEquals("USE_INDEX", indexHint.name().value());
        assertEquals("idx_users_name", assertInstanceOf(IdentifierHintArg.class, indexHint.args().getFirst()).value().value());
    }
}
