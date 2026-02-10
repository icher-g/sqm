package io.sqm.codegen;

import io.sqm.core.Query;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.core.Lexer;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.postgresql.spi.PostgresSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.IdentifierQuoting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates deterministic Java query classes from {@code *.sql} files.
 */
public final class SqlFileCodeGenerator {
    private static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile(":([A-Za-z_][A-Za-z0-9_]*)");
    private static final String JAVA_EXTENSION = ".java";
    private static final String NEWLINE = "\n";
    private static final String INDENT = "    ";
    private static final String CACHE_FILE_NAME = ".sqm-codegen.hashes";
    private static final String GENERATOR_FORMAT_VERSION = "1";
    private static final Comparator<SqlSourceFile> SOURCE_FILE_ORDER = Comparator
        .comparing(SqlSourceFile::methodName)
        .thenComparing(sourceFile -> normalizePath(sourceFile.relativePath()));
    private static final Comparator<SqlFolderGroup> GROUP_ORDER = Comparator
        .comparing(SqlFolderGroup::className)
        .thenComparing(group -> normalizePath(group.folder()));

    private final SqlFileCodegenOptions options;
    private final List<ParseStage> parseStages;
    private final SqmJavaEmitter emitter;

    private SqlFileCodeGenerator(SqlFileCodegenOptions options) {
        this.options = options;
        this.parseStages = parseStagesFor(options.dialect());
        this.emitter = new SqmJavaEmitter();
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

    private static String renderSetLiteral(Collection<String> values) {
        if (values.isEmpty()) {
            return "Set.of()";
        }
        var ordered = new ArrayList<>(values);
        ordered.sort(String::compareTo);
        var joined = ordered.stream()
            .map(value -> "\"" + escapeJavaString(value) + "\"")
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
        return "Set.of(" + joined + ")";
    }

    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String escapeJavaString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String indentContinuationLines(String value, int spaces) {
        var indent = " ".repeat(Math.max(0, spaces));
        return value.replace(NEWLINE, NEWLINE + indent);
    }

    private static String orderedPair(String left, String right) {
        return left.compareTo(right) <= 0
            ? "'" + left + "' and '" + right + "'"
            : "'" + right + "' and '" + left + "'";
    }

    private static List<ParseStage> parseStagesFor(SqlCodegenDialect dialect) {
        var ansi = ParseContext.of(new AnsiSpecs());
        var postgres = ParseContext.of(new PostgresSpecs());
        return switch (dialect) {
            case ANSI -> List.of(
                new ParseStage("ansi", ansi),
                new ParseStage("postgresql", postgres)
            );
            case POSTGRESQL -> List.of(
                new ParseStage("postgresql", postgres),
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
            var query = parseSql(relativePath, sql);
            var params = extractNamedParameters(sql);
            var hash = sha256(sql);
            var folder = relativePath.getParent();
            return new SqlSourceFile(relativePath, folder == null ? Path.of("") : folder, methodName, query, params, hash);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read SQL file " + sqlFile, ex);
        }
    }

    private Query parseSql(Path relativePath, String sql) {
        var attempts = new ArrayList<ParseAttempt>(parseStages.size());
        for (var stage : parseStages) {
            var result = stage.context().parse(Query.class, sql);
            if (result.ok() && result.value() != null) {
                return result.value();
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

    private static int bestProblemPosition(ParseAttempt attempt) {
        return attempt.result().problems().stream()
            .mapToInt(problem -> Math.max(problem.pos(), -1))
            .max()
            .orElse(-1);
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
                Files.writeString(outputFile, renderGroup(group), StandardCharsets.UTF_8);
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

    private String renderGroup(SqlFolderGroup group) {
        var code = new StringBuilder();
        code.append("package ").append(options.basePackage()).append(";").append(NEWLINE).append(NEWLINE);
        code.append("import javax.annotation.processing.Generated;").append(NEWLINE);
        code.append("import io.sqm.core.Query;").append(NEWLINE);
        code.append("import java.util.Set;").append(NEWLINE);
        code.append("import static io.sqm.dsl.Dsl.*;").append(NEWLINE).append(NEWLINE);
        code.append("/**").append(NEWLINE);
        code.append(" * Generated from SQL files located in ").append(normalizePath(group.folder())).append(".").append(NEWLINE);
        code.append(" * Dialect: ").append(options.dialect().name()).append(".").append(NEWLINE);
        code.append(" * Source SQL paths:").append(NEWLINE);
        for (var file : group.files()) {
            code.append(" * - ").append(normalizePath(file.relativePath())).append(NEWLINE);
        }
        code.append(" */").append(NEWLINE);
        code.append(renderGeneratedAnnotation(group)).append(NEWLINE);
        code.append("public final class ").append(group.className()).append(" {").append(NEWLINE).append(NEWLINE);
        code.append(INDENT).append("private ").append(group.className()).append("() {").append(NEWLINE);
        code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
        for (var file : group.files()) {
            String queryExpression;
            try {
                queryExpression = emitter.emitQuery(file.query());
            } catch (IllegalStateException ex) {
                throw new SqlFileCodegenException(normalizePath(file.relativePath()) + ": " + ex.getMessage());
            }
            code.append(INDENT).append("/**").append(NEWLINE);
            code.append(INDENT).append(" * SQL source: ").append(normalizePath(file.relativePath())).append(NEWLINE);
            code.append(INDENT).append(" *").append(NEWLINE);
            code.append(INDENT).append(" * @return query model for this SQL source.").append(NEWLINE);
            code.append(INDENT).append(" */").append(NEWLINE);
            code.append(INDENT).append("public static Query ").append(file.methodName()).append("() {").append(NEWLINE);
            code.append(INDENT).append(INDENT).append("return ")
                .append(indentContinuationLines(queryExpression, 8))
                .append(";").append(NEWLINE);
            code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
            code.append(INDENT).append("/**").append(NEWLINE);
            code.append(INDENT).append(" * Returns named parameters referenced by ").append(normalizePath(file.relativePath())).append(".").append(NEWLINE);
            code.append(INDENT).append(" *").append(NEWLINE);
            code.append(INDENT).append(" * @return immutable set of named parameter identifiers.").append(NEWLINE);
            code.append(INDENT).append(" */").append(NEWLINE);
            code.append(INDENT).append("public static Set<String> ").append(file.methodName()).append("Params() {").append(NEWLINE);
            code.append(INDENT).append(INDENT).append("return ").append(renderSetLiteral(file.parameters())).append(";").append(NEWLINE);
            code.append(INDENT).append("}").append(NEWLINE).append(NEWLINE);
        }
        code.append("}").append(NEWLINE);
        return code.toString();
    }

    private String renderGeneratedAnnotation(SqlFolderGroup group) {
        var comments = new StringBuilder("dialect=")
            .append(options.dialect().name())
            .append("; sqlFolder=")
            .append(normalizePath(group.folder()))
            .append("; sqlFiles=");
        for (int i = 0; i < group.files().size(); i++) {
            if (i > 0) {
                comments.append(",");
            }
            comments.append(normalizePath(group.files().get(i).relativePath()));
        }

        var annotation = new StringBuilder();
        annotation.append("@Generated(").append(NEWLINE);
        annotation.append(INDENT).append("value = \"io.sqm.codegen.SqlFileCodeGenerator\",").append(NEWLINE);
        annotation.append(INDENT).append("comments = \"").append(escapeJavaString(comments.toString())).append("\"");
        if (options.includeGenerationTimestamp()) {
            annotation.append(",").append(NEWLINE);
            annotation.append(INDENT).append("date = \"").append(Instant.now().toString()).append("\"").append(NEWLINE);
            annotation.append(")");
            return annotation.toString();
        }
        annotation.append(NEWLINE).append(")");
        return annotation.toString();
    }
}

record SqlSourceFile(Path relativePath, Path folder, String methodName, Query query, Set<String> parameters, String sqlHash) {
}

record SqlFolderGroup(Path folder, String className, List<SqlSourceFile> files) {
}

record ParseStage(String name, ParseContext context) {
}

record ParseAttempt(String stage, ParseResult<? extends Query> result, IdentifierQuoting identifierQuoting) {
}

record LineColumn(int line, int column) {
}
