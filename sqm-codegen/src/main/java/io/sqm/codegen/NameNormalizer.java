package io.sqm.codegen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class NameNormalizer {
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[^A-Za-z0-9]+");

    private NameNormalizer() {
    }

    static String toClassName(Path folder) {
        if (folder.toString().isBlank()) {
            return "RootQueries";
        }
        var tokens = new ArrayList<String>();
        for (var segment : folder) {
            tokens.addAll(splitToTokens(segment.toString()));
        }
        if (tokens.isEmpty()) {
            return "RootQueries";
        }
        var className = new StringBuilder();
        for (var token : tokens) {
            className.append(capitalize(token));
        }
        if (!Character.isJavaIdentifierStart(className.charAt(0))) {
            className.insert(0, 'Q');
        }
        return className.append("Queries").toString();
    }

    static String toMethodName(String fileNameWithoutExtension) {
        var tokens = splitToTokens(fileNameWithoutExtension);
        if (tokens.isEmpty()) {
            return "query";
        }
        var methodName = new StringBuilder(tokens.getFirst().toLowerCase(Locale.ROOT));
        for (int i = 1; i < tokens.size(); i++) {
            methodName.append(capitalize(tokens.get(i)));
        }
        if (!Character.isJavaIdentifierStart(methodName.charAt(0))) {
            methodName.insert(0, 'q');
        }
        for (int i = 1; i < methodName.length(); i++) {
            if (!Character.isJavaIdentifierPart(methodName.charAt(i))) {
                methodName.setCharAt(i, '_');
            }
        }
        return methodName.toString();
    }

    private static List<String> splitToTokens(String value) {
        var parts = SEPARATOR_PATTERN.split(value);
        var tokens = new ArrayList<String>(parts.length);
        for (var part : parts) {
            if (!part.isBlank()) {
                tokens.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return tokens;
    }

    private static String capitalize(String token) {
        if (token.isEmpty()) {
            return token;
        }
        if (token.length() == 1) {
            return token.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(token.charAt(0)) + token.substring(1);
    }
}
