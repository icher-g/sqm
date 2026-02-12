package io.sqm.codegen.maven;

import io.sqm.schema.introspect.SchemaProvider;
import io.sqm.schema.introspect.snapshot.JsonSchemaProvider;
import io.sqm.validate.schema.model.DbSchema;
import org.apache.maven.plugin.logging.Log;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

/**
 * Schema provider that reuses a JSON cache snapshot for JDBC introspection
 * when cache freshness and metadata constraints are satisfied.
 */
final class CachingSchemaProvider implements SchemaProvider {
    private final SchemaProvider delegate;
    private final DataSource dataSource;
    private final Path cachePath;
    private final boolean refresh;
    private final boolean writeCache;
    private final long ttlMinutes;
    private final SchemaCacheMetadata expectedMetadata;
    private final Log log;

    /**
     * Creates caching schema provider.
     *
     * @param delegate         schema provider used for JDBC introspection.
     * @param dataSource       JDBC data source used to read DB metadata.
     * @param cachePath        cache snapshot path.
     * @param refresh          if {@code true}, always bypass cache.
     * @param writeCache       if {@code true}, write refreshed cache.
     * @param ttlMinutes       cache TTL in minutes, {@code <= 0} disables expiration.
     * @param expectedMetadata metadata constraints used for cache reuse checks.
     * @param log              Maven logger.
     */
    CachingSchemaProvider(
        SchemaProvider delegate,
        DataSource dataSource,
        Path cachePath,
        boolean refresh,
        boolean writeCache,
        long ttlMinutes,
        SchemaCacheMetadata expectedMetadata,
        Log log
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.cachePath = Objects.requireNonNull(cachePath, "cachePath");
        this.refresh = refresh;
        this.writeCache = writeCache;
        this.ttlMinutes = ttlMinutes;
        this.expectedMetadata = Objects.requireNonNull(expectedMetadata, "expectedMetadata");
        this.log = Objects.requireNonNull(log, "log");
    }

    private static Integer parseInteger(String value) {
        var normalized = normalizeBlank(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static long parseLong(String value) {
        var normalized = normalizeBlank(value);
        if (normalized == null) {
            return 0L;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public DbSchema load() throws SQLException {
        if (!refresh && canUseCache()) {
            log.info("SQM SQL codegen schema: using cached snapshot " + cachePath);
            return JsonSchemaProvider.of(cachePath).load();
        }
        log.info("SQM SQL codegen schema: loading from JDBC metadata.");
        var actualMetadata = readCurrentMetadata();
        var schema = delegate.load();
        if (writeCache) {
            try {
                var parent = cachePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                JsonSchemaProvider.of(cachePath).save(schema);
                writeMetadata(actualMetadata);
                log.info("SQM SQL codegen schema: updated cache " + cachePath);
            } catch (IOException ex) {
                throw new SQLException("Failed to write schema cache to " + cachePath, ex);
            }
        }
        return schema;
    }

    private boolean canUseCache() {
        if (!Files.exists(cachePath)) {
            return false;
        }
        if (isExpired()) {
            log.info("SQM SQL codegen schema cache expired: " + cachePath);
            return false;
        }
        var metadata = readMetadata();
        if (metadata == null) {
            log.info("SQM SQL codegen schema cache metadata is missing, reusing cache for backward compatibility.");
            return true;
        }
        if (!metadata.matchesExpected(expectedMetadata)) {
            log.info("SQM SQL codegen schema cache metadata mismatch. Cache will be refreshed.");
            return false;
        }
        return true;
    }

    private boolean isExpired() {
        if (ttlMinutes <= 0) {
            return false;
        }
        try {
            var modifiedTime = Files.getLastModifiedTime(cachePath);
            var expiresAt = modifiedTime.toInstant().plus(Duration.ofMinutes(ttlMinutes));
            return Instant.now().isAfter(expiresAt);
        } catch (IOException ex) {
            return true;
        }
    }

    private SchemaCacheMetadata readCurrentMetadata() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            var md = connection.getMetaData();
            return new SchemaCacheMetadata(
                expectedMetadata.dialect(),
                normalizeBlank(md.getDatabaseProductName()),
                md.getDatabaseMajorVersion(),
                System.currentTimeMillis()
            );
        }
    }

    private Path metadataPath() {
        return Path.of(cachePath + ".meta.properties");
    }

    private SchemaCacheMetadata readMetadata() {
        var metadataPath = metadataPath();
        if (!Files.exists(metadataPath)) {
            return null;
        }
        var props = new Properties();
        try (var input = Files.newInputStream(metadataPath)) {
            props.load(input);
            return new SchemaCacheMetadata(
                normalizeBlank(props.getProperty("dialect")),
                normalizeBlank(props.getProperty("databaseProduct")),
                parseInteger(props.getProperty("databaseMajorVersion")),
                parseLong(props.getProperty("generatedAtEpochMillis"))
            );
        } catch (IOException ex) {
            return null;
        }
    }

    private void writeMetadata(SchemaCacheMetadata metadata) throws IOException {
        var metadataPath = metadataPath();
        var parent = metadataPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        var props = new Properties();
        props.setProperty("formatVersion", "1");
        if (metadata.dialect() != null) {
            props.setProperty("dialect", metadata.dialect());
        }
        if (metadata.databaseProduct() != null) {
            props.setProperty("databaseProduct", metadata.databaseProduct());
        }
        if (metadata.databaseMajorVersion() != null) {
            props.setProperty("databaseMajorVersion", Integer.toString(metadata.databaseMajorVersion()));
        }
        props.setProperty("generatedAtEpochMillis", Long.toString(metadata.generatedAtEpochMillis()));
        try (var output = Files.newOutputStream(metadataPath)) {
            props.store(output, "SQM SQL codegen schema cache metadata");
        }
        if (metadata.generatedAtEpochMillis() > 0L) {
            Files.setLastModifiedTime(cachePath, FileTime.fromMillis(metadata.generatedAtEpochMillis()));
        }
    }
}

