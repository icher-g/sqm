package io.sqm.codegen;

import io.sqm.core.Statement;
import io.sqm.core.StatementSequence;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Lexer;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.mysql.spi.MySqlSpecs;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.IdentifierQuoting;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.mysql.MySqlValidationDialect;
import io.sqm.validate.postgresql.PostgresValidationDialect;
import io.sqm.validate.schema.SchemaStatementValidator;
import io.sqm.validate.sqlserver.SqlServerValidationDialect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates deterministic Java statement classes from {@code *.sql} files.
 */
public final class SqlFileCodeGenerator {
    private static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile(":([A-Za-z_][A-Za-z0-9_]*)");
    private static final String JAVA_EXTENSION = ".java";
    private static final String CACHE_FILE_NAME = ".sqm-codegen.hashes";
    private static final String GENERATOR_FORMAT_VERSION = "2";
    private static final Comparator<SqlSourceFile> SOURCE_FILE_ORDER = Comparator
        .comparing(SqlSourceFile::methodName)
        .thenComparing(sourceFile -> normalizePath(sourceFile.relativePath()));
    private static final Comparator<SqlFolderGroup> GROUP_ORDER = Comparator
        .comparing(SqlFolderGroup::className)
        .thenComparing(group -> normalizePath(group.folder()));

    private final SqlFileCodegenOptions options;
    private final List<ParseStage> parseStages;
    private final SchemaStatementValidator schemaValidator;
    private final List<SqlValidationIssue> validationIssues;
    private final SqmDslRenderer dslRenderer;

    private SqlFileCodeGenerator(SqlFileCodegenOptions options) {
        this.options = options;
        this.parseStages = parseStagesFor(options.dialect());
        this.schemaValidator = createSchemaValidator(options);
        this.validationIssues = new ArrayList<>();
        this.dslRenderer = new SqmDslRenderer(options);
    }

    /**
     * Creates a code generator with the provided options.
     *
     * @param options generation options.
     * @return new code generator instance.
     */
    public static SqlFileCodeGenerator of(SqlFileCodegenOptions options) {
        return new SqlFileCodeGenerator(Objects.requireNonNull(options, "options"));
    }

    private static SqlFileCodegenException parseError(Path relativePath, String sql, ParseAttempt attempt) {
        var result = attempt.result();
        var firstProblem = result.problems().getFirst();
        var lineColumn = toLineColumn(sql, firstProblem.pos());
        var token = tokenAt(sql, firstProblem.pos(), attempt.identifierQuoting());
        var message = normalizePath(relativePath)
            + ":"
            + lineColumn.line()
            + ":"
            + lineColumn.column()
            + " stage="
            + attempt.stage()
            + " token="
            + token
            + " "
            + firstProblem.message();
        return new SqlFileCodegenException(message);
    }


    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }


    private static String orderedPair(String left, String right) {
        return left.compareTo(right) <= 0
            ? "'" + left + "' and '" + right + "'"
            : "'" + right + "' and '" + left + "'";
    }

    private static List<ParseStage> parseStagesFor(SqlCodegenDialect dialect) {
        var ansi = ParseContext.of(new AnsiSpecs());
        var postgres = ParseContext.of(new PostgresSpecs());
        var mysql = ParseContext.of(new MySqlSpecs());
        var sqlServer = ParseContext.of(new SqlServerSpecs());
        return switch (dialect) {
            case ANSI -> List.of(
                new ParseStage("ansi", ansi),
                new ParseStage("postgresql", postgres),
                new ParseStage("mysql", mysql),
                new ParseStage("sqlserver", sqlServer)
            );
            case POSTGRESQL -> List.of(
                new ParseStage("postgresql", postgres),
                new ParseStage("ansi", ansi)
            );
            case MYSQL -> List.of(
                new ParseStage("mysql", mysql),
                new ParseStage("ansi", ansi)
            );
            case SQLSERVER -> List.of(
                new ParseStage("sqlserver", sqlServer),
                new ParseStage("ansi", ansi)
            );
        };
    }

    private static String tokenAt(String sql, int pos, IdentifierQuoting quoting) {
        if (pos < 0) {
            return "<unknown>";
        }
        try {
            var tokens = Lexer.lexAll(sql, quoting);
            if (tokens.isEmpty()) {
                return "<unknown>";
            }
            Token matched = null;
            for (int i = 0; i < tokens.size(); i++) {
                var current = tokens.get(i);
                if (current.pos() == pos) {
                    matched = current;
                    break;
                }
                var nextPos = (i + 1 < tokens.size()) ? tokens.get(i + 1).pos() : Integer.MAX_VALUE;
                if (current.pos() <= pos && pos < nextPos) {
                    matched = current;
                    break;
                }
            }
            if (matched == null) {
                matched = tokens.getLast();
            }
            if (matched.type() == TokenType.EOF) {
                return "EOF";
            }
            var lexeme = matched.lexeme() == null ? "" : matched.lexeme();
            if (lexeme.length() > 40) {
                lexeme = lexeme.substring(0, 37) + "...";
            }
            lexeme = lexeme.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
            return matched.type().name() + "[" + lexeme + "]";
        } catch (RuntimeException ex) {
            return "<unknown>";
        }
    }

    private static LineColumn toLineColumn(String sql, int pos) {
        if (pos < 0) {
            return new LineColumn(1, 1);
        }
        int line = 1;
        int column = 1;
        int max = Math.min(pos, sql.length());
        for (int i = 0; i < max; i++) {
            char ch = sql.charAt(i);
            if (ch == '\n') {
                line++;
                column = 1;
            }
            else {
                column++;
            }
        }
        return new LineColumn(line, column);
    }

    private static SchemaStatementValidator createSchemaValidator(SqlFileCodegenOptions options) {
        var provider = options.schemaProvider().orElse(null);
        if (provider == null) {
            return null;
        }
        try {
            var schema = provider.load();
            return switch (options.dialect()) {
                case ANSI -> SchemaStatementValidator.of(schema);
                case POSTGRESQL -> SchemaStatementValidator.of(schema, PostgresValidationDialect.of());
                case MYSQL -> SchemaStatementValidator.of(schema, MySqlValidationDialect.of());
                case SQLSERVER -> SchemaStatementValidator.of(schema, SqlServerValidationDialect.of());
            };
        } catch (SQLException ex) {
            throw new SqlFileCodegenException("Failed to load schema for validation: " + ex.getMessage());
        }
    }

    private static String formatProblem(ValidationProblem problem) {
        var details = new StringBuilder(problem.code().name())
            .append(": ")
            .append(problem.message());
        if (problem.clausePath() != null && !problem.clausePath().isBlank()) {
            details.append(" [clause=").append(problem.clausePath()).append("]");
        }
        if (problem.nodeKind() != null && !problem.nodeKind().isBlank()) {
            details.append(" [node=").append(problem.nodeKind()).append("]");
        }
        return details.toString();
    }

    private static int bestProblemPosition(ParseAttempt attempt) {
        return attempt.result().problems().stream()
            .mapToInt(problem -> Math.max(problem.pos(), -1))
            .max()
            .orElse(-1);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    /**
     * Returns semantic validation issues collected during generation.
     * The list is non-empty only when schema validation is enabled and
     * {@link SqlFileCodegenOptions#failOnValidationError()} is {@code false}.
     *
     * @return immutable list of collected semantic validation issues.
     */
    public List<SqlValidationIssue> validationIssues() {
        return List.copyOf(validationIssues);
    }

    /**
     * Generates Java source files from SQL files.
     *
     * @return paths of generated files in deterministic order.
     */
    public List<Path> generate() {
        var groups = new ArrayList<>(scanGroups());
        groups.sort(GROUP_ORDER);
        var oldCache = readCache();
        var newCache = new LinkedHashMap<String, String>();
        var outputFiles = new ArrayList<Path>(groups.size());
        for (var group : groups) {
            outputFiles.add(writeGroup(group, oldCache, newCache));
        }
        writeCache(newCache);
        return List.copyOf(outputFiles);
    }

    private List<SqlFolderGroup> scanGroups() {
        var sourceDir = options.sqlDirectory();
        if (!Files.exists(sourceDir)) {
            return List.of();
        }
        var sqlFiles = readSqlFiles(sourceDir);
        if (sqlFiles.isEmpty()) {
            return List.of();
        }
        var classNameToFolder = new LinkedHashMap<String, Path>();
        var folderToEntries = new LinkedHashMap<Path, List<SqlSourceFile>>();
        for (var file : sqlFiles) {
            var className = NameNormalizer.toClassName(file.folder());
            var existingFolder = classNameToFolder.putIfAbsent(className, file.folder());
            if (existingFolder != null && !existingFolder.equals(file.folder())) {
                throw new SqlFileCodegenException(
                    "Class name collision: " + className + " mapped from both "
                        + orderedPair(normalizePath(existingFolder), normalizePath(file.folder()))
                );
            }
            folderToEntries.computeIfAbsent(file.folder(), unused -> new ArrayList<>()).add(file);
        }
        var groups = new ArrayList<SqlFolderGroup>(folderToEntries.size());
        for (var entry : folderToEntries.entrySet()) {
            var methodsByName = new LinkedHashMap<String, Path>();
            var files = new ArrayList<>(entry.getValue());
            files.sort(SOURCE_FILE_ORDER);
            for (var file : files) {
                var existingPath = methodsByName.putIfAbsent(file.methodName(), file.relativePath());
                if (existingPath != null && !existingPath.equals(file.relativePath())) {
                    throw new SqlFileCodegenException(
                        "Method name collision in class " + NameNormalizer.toClassName(entry.getKey())
                            + ": method '" + file.methodName() + "' mapped from both "
                            + orderedPair(normalizePath(existingPath), normalizePath(file.relativePath()))
                    );
                }
            }
            groups.add(new SqlFolderGroup(entry.getKey(), NameNormalizer.toClassName(entry.getKey()), List.copyOf(files)));
        }
        return groups;
    }

    private List<SqlSourceFile> readSqlFiles(Path sourceDir) {
        var files = new ArrayList<SqlSourceFile>();
        try (var stream = Files.walk(sourceDir)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
                .sorted(Comparator.comparing(path -> normalizePath(sourceDir.relativize(path))))
                .forEach(path -> files.add(toSourceFile(sourceDir, path)));
            return files;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan SQL files in " + sourceDir, ex);
        }
    }

    private SqlSourceFile toSourceFile(Path sourceDir, Path sqlFile) {
        var relativePath = sourceDir.relativize(sqlFile);
        var fileName = relativePath.getFileName().toString();
        var baseName = fileName.substring(0, fileName.length() - 4);
        var methodName = NameNormalizer.toMethodName(baseName);
        try {
            var sql = Files.readString(sqlFile, StandardCharsets.UTF_8);
            var statements = parseSql(relativePath, sql);
            var params = extractNamedParameters(sql);
            var hash = sha256(sql);
            var folder = relativePath.getParent();
            return new SqlSourceFile(relativePath, folder == null ? Path.of("") : folder, methodName, params, hash, statements);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read SQL file " + sqlFile, ex);
        }
    }

    private List<Statement> parseSql(Path relativePath, String sql) {
        var attempts = new ArrayList<ParseAttempt>(parseStages.size());
        for (var stage : parseStages) {
            var result = stage.context().parse(StatementSequence.class, sql);
            if (result.ok() && result.value() != null) {
                var statements = result.value().statements();
                if (statements.isEmpty()) {
                    throw new SqlFileCodegenException(normalizePath(relativePath) + ":1:1 SQL file does not contain a statement.");
                }
                for (var statement : statements) {
                    validateStatement(relativePath, statement);
                }
                return statements;
            }
            attempts.add(new ParseAttempt(stage.name(), result, stage.context().identifierQuoting()));
        }
        if (attempts.isEmpty()) {
            throw new SqlFileCodegenException(
                normalizePath(relativePath) + ":1:1 stage=unknown token=<unknown> Unable to parse SQL."
            );
        }
        var bestAttempt = attempts.stream()
            .max(Comparator.comparingInt(SqlFileCodeGenerator::bestProblemPosition))
            .orElse(attempts.getFirst());
        throw parseError(relativePath, sql, bestAttempt);
    }

    private void validateStatement(Path relativePath, Statement statement) {
        if (schemaValidator == null) {
            return;
        }
        var result = schemaValidator.validate(statement);
        if (result.ok()) {
            return;
        }
        var firstProblem = result.problems().getFirst();
        validationIssues.add(new SqlValidationIssue(relativePath, firstProblem));
        if (!options.failOnValidationError()) {
            return;
        }
        throw new SqlFileCodegenException(
            normalizePath(relativePath)
                + ": semantic validation failed: "
                + formatProblem(firstProblem)
        );
    }

    private Set<String> extractNamedParameters(String sql) {
        var params = new LinkedHashSet<String>();
        Matcher matcher = NAMED_PARAMETER_PATTERN.matcher(sql);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }
        return Set.copyOf(params);
    }

    private Path writeGroup(SqlFolderGroup group, Map<String, String> oldCache, Map<String, String> newCache) {
        var packagePath = options.basePackage().replace('.', '/');
        var outputFile = options.generatedSourcesDirectory()
            .resolve(packagePath)
            .resolve(group.className() + JAVA_EXTENSION);
        var cacheKey = groupCacheKey(group);
        var groupFingerprint = groupFingerprint(group);
        var unchanged = groupFingerprint.equals(oldCache.get(cacheKey)) && Files.exists(outputFile);

        try {
            Files.createDirectories(Objects.requireNonNull(outputFile.getParent(), "outputFile parent"));
            if (!unchanged) {
                Files.writeString(outputFile, dslRenderer.render(group), StandardCharsets.UTF_8);
            }
            newCache.put(cacheKey, groupFingerprint);
            return outputFile;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write generated source " + outputFile, ex);
        }
    }

    private Map<String, String> readCache() {
        var cacheFile = options.generatedSourcesDirectory().resolve(CACHE_FILE_NAME);
        if (!Files.exists(cacheFile)) {
            return Map.of();
        }
        var properties = new Properties();
        try (var input = Files.newInputStream(cacheFile)) {
            properties.load(input);
            var cache = new LinkedHashMap<String, String>(properties.size());
            for (var key : properties.stringPropertyNames()) {
                cache.put(key, properties.getProperty(key));
            }
            return cache;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read SQM codegen cache " + cacheFile, ex);
        }
    }

    private void writeCache(Map<String, String> cache) {
        var cacheFile = options.generatedSourcesDirectory().resolve(CACHE_FILE_NAME);
        var properties = new Properties();
        properties.putAll(cache);
        try {
            Files.createDirectories(options.generatedSourcesDirectory());
            try (var output = Files.newOutputStream(cacheFile)) {
                properties.store(output, "SQM SQL codegen hash cache");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write SQM codegen cache " + cacheFile, ex);
        }
    }

    private String groupCacheKey(SqlFolderGroup group) {
        return options.basePackage() + "|" + normalizePath(group.folder()) + "|" + group.className();
    }

    private String groupFingerprint(SqlFolderGroup group) {
        var sb = new StringBuilder();
        sb.append("format=").append(GENERATOR_FORMAT_VERSION);
        sb.append(";dialect=").append(options.dialect().name());
        sb.append(";basePackage=").append(options.basePackage());
        sb.append(";includeGenerationTimestamp=").append(options.includeGenerationTimestamp());
        sb.append(";class=").append(group.className());
        sb.append(";folder=").append(normalizePath(group.folder()));
        for (var file : group.files()) {
            sb.append(";file=").append(normalizePath(file.relativePath()));
            sb.append("@method=").append(file.methodName());
            sb.append("@hash=").append(file.sqlHash());
        }
        return sha256(sb.toString());
    }
}

