/*
 * Copyright 2024-2025 The Compress4J Project
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.CompressorInputStream;

/**
 * This abstract class is the superclass of all classes providing data decompression functionality. It provides core
 * methods for decompressing data from various input streams.
 *
 * @param <I> The type of {@link CompressorInputStream} to read compressed data from.
 * @since 2.2
 */
public abstract class Decompressor<I extends CompressorInputStream> implements Closeable {
    /** Compressor input stream to be used for decompression. */
    protected final I compressorInputStream;

    /**
     * Create an instance of {@link Decompressor}
     *
     * @param compressorInputStream the {@link CompressorInputStream} to read from.
     */
    protected Decompressor(I compressorInputStream) {
        this.compressorInputStream = compressorInputStream;
    }

    /**
     * Create a new Decompressor with the given input stream and options.
     *
     * @param builder the compressor input stream builder
     * @param <B> The type of {@link Decompressor.DecompressorBuilder} to build from.
     * @param <D> The type of the {@link Decompressor} to instantiate.
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    protected <B extends Decompressor.DecompressorBuilder<I, D, B>, D extends Decompressor<I>> Decompressor(B builder)
            throws IOException {
        this(builder.buildCompressorInputStream());
    }

    /**
     * Writes all bytes from a file to this output stream.
     *
     * @param file the path to the source file.
     * @return the number of bytes written
     * @throws IOException if an I/O error occurred
     */
    public long write(final File file) throws IOException {
        return write(file.toPath());
    }

    /**
     * Writes all bytes from a path to this output stream.
     *
     * @param path the path to the source file.
     * @return the number of bytes written
     * @throws IOException if an I/O error occurred
     */
    public long write(final Path path) throws IOException {
        return Files.copy(compressorInputStream, path);
    }

    /**
     * Closes the decompressor input stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        compressorInputStream.close();
    }

    /**
     * Build and instance of {@link Decompressor}
     *
     * @param <I> The type of {@link CompressorInputStream} to read entries from.
     * @param <D> The type of {@link Decompressor}
     * @param <B> The type of {@link Decompressor.DecompressorBuilder}
     */
    public abstract static class DecompressorBuilder<
            I extends CompressorInputStream,
            D extends Decompressor<I>,
            B extends Decompressor.DecompressorBuilder<I, D, B>> {

        /** Input stream to read from for decompression. */
        protected final InputStream inputStream;

        /**
         * Constructor that takes an input stream to read from.
         *
         * @param inputStream the input stream to read from
         */
        protected DecompressorBuilder(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        /**
         * Returns the input stream to be used for decompression.
         *
         * @return the input stream
         * @throws IOException if an I/O error occurs while building the compressor input stream
         */
        public abstract I buildCompressorInputStream() throws IOException;

        /**
         * Returns the input stream to be used for decompression.
         *
         * @return the input stream
         */
        protected abstract B getThis();

        /**
         * Builds an instance of {@link Decompressor}.
         *
         * @return a new instance of {@link Decompressor}
         * @throws IOException if an I/O error occurs
         */
        public abstract D build() throws IOException;
    }
}
