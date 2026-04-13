package io.sqm.parser.spi;

import java.util.ArrayList;

/**
 * Parser-local source location support used to attach line and column to parse diagnostics.
 */
public final class ParseLocations {
    private static final ThreadLocal<ParseLocationResolver> CURRENT = new ThreadLocal<>();

    private ParseLocations() {
    }

    /**
     * Opens a parser-local source location scope for the provided SQL text.
     *
     * @param spec SQL text being parsed
     * @return scope that restores the previous resolver when closed
     */
    public static Scope open(String spec) {
        var previous = CURRENT.get();
        CURRENT.set(new ParseLocationResolver(spec));
        return new Scope(previous);
    }

    /**
     * Resolves a source offset into a one-based line and column using the current parse scope.
     *
     * @param sourceOffset zero-based source character offset
     * @return resolved line and column, or {@code null} values when unavailable
     */
    public static LineColumn locate(int sourceOffset) {
        var resolver = CURRENT.get();
        return resolver == null ? new LineColumn(null, null) : resolver.locate(sourceOffset);
    }

    /**
     * One-based source line and column pair.
     *
     * @param line   one-based source line
     * @param column one-based source column
     */
    public record LineColumn(Integer line, Integer column) {
    }

    /**
     * Auto-closeable scope that restores the previous parser-local location resolver.
     */
    public static final class Scope implements AutoCloseable {
        private final ParseLocationResolver previous;

        private Scope(ParseLocationResolver previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                CURRENT.remove();
            }
            else {
                CURRENT.set(previous);
            }
        }
    }

    private static final class ParseLocationResolver {
        private final int[] lineStarts;

        private ParseLocationResolver(String spec) {
            var starts = new ArrayList<Integer>();
            starts.add(0);
            for (var index = 0; index < spec.length(); index++) {
                var current = spec.charAt(index);
                if (current == '\r') {
                    if (index + 1 < spec.length() && spec.charAt(index + 1) == '\n') {
                        index++;
                    }
                    starts.add(index + 1);
                    continue;
                }
                if (current == '\n') {
                    starts.add(index + 1);
                }
            }
            this.lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
        }

        private LineColumn locate(int sourceOffset) {
            if (sourceOffset < 0) {
                return new LineColumn(null, null);
            }
            var lineIndex = findLineIndex(sourceOffset);
            var lineStart = lineStarts[lineIndex];
            return new LineColumn(lineIndex + 1, sourceOffset - lineStart + 1);
        }

        private int findLineIndex(int sourceOffset) {
            var low = 0;
            var high = lineStarts.length - 1;
            while (low <= high) {
                var mid = (low + high) >>> 1;
                var start = lineStarts[mid];
                if (start == sourceOffset) {
                    return mid;
                }
                if (start < sourceOffset) {
                    low = mid + 1;
                }
                else {
                    high = mid - 1;
                }
            }
            return Math.max(0, high);
        }
    }
}
