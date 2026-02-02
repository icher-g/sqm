package io.sqm.parser.ansi;

import io.sqm.core.dialect.DialectCapabilities;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.VersionedDialectCapabilities;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.Lookups;
import io.sqm.parser.spi.ParsersRepository;
import io.sqm.parser.spi.Specs;

import java.util.Objects;

/**
 * Test-specific {@link Specs} implementation that enables all SQL features.
 * <p>
 * This class is used to test parser implementations without dialect restrictions.
 * By enabling all features, tests can verify the actual parsing logic rather than
 * just feature rejection behavior.
 * </p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * ParseContext ctx = ParseContext.of(new TestSpecs());
 * var result = ctx.parse(DollarStringLiteralExpr.class, "$$text$$");
 * assertTrue(result.ok()); // Now we can test actual parsing logic
 * }</pre>
 */
public class TestSpecs implements Specs {

    private Lookups lookups;
    private IdentifierQuoting identifierQuoting;
    private DialectCapabilities capabilities;
    private final SqlDialectVersion version;

    /**
     * Creates test specs with default SQL:2016 version and all features enabled.
     */
    public TestSpecs() {
        this(SqlDialectVersion.of(2016));
    }

    /**
     * Creates test specs for a specific dialect version with all features enabled.
     *
     * @param version SQL version used for feature availability
     */
    public TestSpecs(SqlDialectVersion version) {
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public ParsersRepository parsers() {
        return Parsers.ansi();
    }

    @Override
    public Lookups lookups() {
        if (lookups == null) {
            lookups = new AnsiLookups();
        }
        return lookups;
    }

    @Override
    public IdentifierQuoting identifierQuoting() {
        if (identifierQuoting == null) {
            identifierQuoting = IdentifierQuoting.of('"');
        }
        return identifierQuoting;
    }

    /**
     * Returns dialect capabilities with ALL features enabled.
     * <p>
     * This allows testing parser implementations without feature gate restrictions.
     * </p>
     *
     * @return dialect capabilities with all features enabled
     */
    @Override
    public DialectCapabilities capabilities() {
        if (capabilities == null) {
            var builder = VersionedDialectCapabilities.builder(version);
            // Enable all features for testing
            for (SqlFeature feature : SqlFeature.values()) {
                builder.supports(feature);
            }
            capabilities = builder.build();
        }
        return capabilities;
    }
}
