package io.sqm.control;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LoggingAuditEventPublisherTest {

    @Test
    void validates_factory_arguments() {
        var logger = Logger.getLogger("io.sqm.control.test.audit");
        assertThrows(NullPointerException.class, () -> LoggingAuditEventPublisher.of(null));
        assertThrows(NullPointerException.class, () -> LoggingAuditEventPublisher.of(logger, null));
    }

    @Test
    void logs_audit_event_with_expected_level_and_message() {
        var logger = Logger.getLogger("io.sqm.control.test.audit.publisher");
        logger.setUseParentHandlers(false);
        var records = new ArrayList<LogRecord>();
        var handler = new CollectingHandler(records);
        logger.addHandler(handler);
        try {
            var publisher = LoggingAuditEventPublisher.of(logger, Level.WARNING);
            var event = new AuditEvent(
                "select 1",
                "select 1",
                List.of(),
                null,
                DecisionResult.allow(),
                ExecutionContext.of("postgresql", ExecutionMode.ANALYZE),
                1L
            );

            publisher.publish(event);

            assertEquals(1, records.size());
            var record = records.getFirst();
            assertEquals(Level.WARNING, record.getLevel());
            assertNotNull(record.getMessage());
            assertTrue(record.getMessage().contains("sqm-audit decision=ALLOW"));
            assertTrue(record.getMessage().contains("sql=select 1"));
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static final class CollectingHandler extends Handler {
        private final List<LogRecord> records;

        private CollectingHandler(List<LogRecord> records) {
            this.records = records;
        }

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
