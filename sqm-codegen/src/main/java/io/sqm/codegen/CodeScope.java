package io.sqm.codegen;

import java.io.Closeable;

/**
 * Temporarily opens a formatting scope for generated DSL code.
 */
public class CodeScope implements Closeable {
    private final CodeBuilder builder;
    private final boolean inline;

    /**
     * Creates a formatting scope around the provided code builder.
     *
     * @param builder target code builder
     * @param inline  whether the scoped code should stay inline
     */
    public CodeScope(CodeBuilder builder, boolean inline) {
        this.builder = builder;
        this.inline = inline;
        if (!this.inline) {
            this.builder.nl().in();
        }
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     */
    @Override
    public void close() {
        if (!this.inline) {
            this.builder.out().nl();
        }
    }
}
