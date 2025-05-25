/*
 * Copyright 2025 The Compress4J Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.compress4j.compressors;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.CompressorOutputStream;

/**
 * This abstract class is the superclass of all classes providing compression.
 *
 * @param <O> The type of {@link CompressorOutputStream} to write to.
 * @since 2.2
 */
public abstract class Compressor<O extends CompressorOutputStream<? extends OutputStream>> implements AutoCloseable {
    /** Compressor output stream to be used for compression. */
    protected final O compressorOutputStream;

    /**
     * Create an instance of {@link Compressor}
     *
     * @param compressorOutputStream the {@link CompressorOutputStream} to write to.
     */
    protected Compressor(O compressorOutputStream) {
        this.compressorOutputStream = compressorOutputStream;
    }

    /**
     * Create a new Compressor with the given output stream and options.
     *
     * @param builder the compressor output stream builder
     * @param <B> The type of {@link Compressor.CompressorBuilder} to build from.
     * @param <C> The type of the {@link Compressor} to instantiate.
     * @throws IOException if an I/O error occurred
     */
    protected <B extends Compressor.CompressorBuilder<O, B, C>, C extends Compressor<O>> Compressor(B builder)
            throws IOException {
        this(builder.buildCompressorOutputStream());
    }

    /**
     * Writes all bytes from a file this output stream.
     *
     * @param file the path to the source file.
     * @return the number of bytes read or written.
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public long write(final File file) throws IOException {
        return write(file.toPath());
    }

    /**
     * Writes all bytes from a file to this output stream.
     *
     * @param path the path to the source file.
     * @return the number of bytes read or written.
     * @throws IOException if an I/O error occurs when reading or writing.
     */
    public long write(final Path path) throws IOException {
        return Files.copy(path, compressorOutputStream);
    }

    @Override
    public void close() throws Exception {
        compressorOutputStream.close();
    }

    /**
     * Build and instance of {@link Compressor}
     *
     * @param <O> The type of {@link CompressorOutputStream} to write entries to.
     * @param <B> The type of {@link Compressor.CompressorBuilder}
     * @param <C> The type of {@link Compressor}
     */
    public abstract static class CompressorBuilder<
            O extends CompressorOutputStream<? extends OutputStream>,
            B extends Compressor.CompressorBuilder<O, B, C>,
            C extends Compressor<O>> {
        protected final OutputStream outputStream;

        /**
         * Create a new {@link Compressor.CompressorBuilder} with the given output stream.
         *
         * @param outputStream the output stream
         */
        protected CompressorBuilder(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        /**
         * get the current instance of the object
         *
         * @return current instance
         */
        protected abstract B getThis();

        /**
         * Start a new compressor. Entries can be included in the compressor using the putEntry method, and then the
         * compressor should be closed using its close method. In addition, options can be applied to the underlying
         * stream. E.g. archiving level.
         *
         * <ol>
         *   <li>Use {@link #outputStream} as underlying output stream to which to write the compressor.
         * </ol>
         *
         * @return new compressor object for use in putEntry
         * @throws IOException thrown by the underlying output stream for I/O errors
         */
        public abstract O buildCompressorOutputStream() throws IOException;

        /**
         * Use this method to build an instance of the {@link Compressor}, use
         * {@link Compressor#Compressor(Compressor.CompressorBuilder)} to pass in instance of this builder
         *
         * @return an instance of the {@link Compressor}
         * @throws IOException thrown by the underlying output stream for I/O errors
         */
        public abstract C build() throws IOException;
    }
}
