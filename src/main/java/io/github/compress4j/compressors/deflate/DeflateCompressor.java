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
package io.github.compress4j.compressors.deflate;

import io.github.compress4j.compressors.Compressor;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateParameters;

/**
 * This class provides a Deflate compressor that writes to a DeflateCompressorOutputStream. It extends the Compressor
 * class and provides a builder for creating instances.
 *
 * @since 2.2
 */
public class DeflateCompressor extends Compressor<DeflateCompressorOutputStream> {

    /**
     * Constructor that initializes the DeflateCompressor with a DeflateCompressorOutputStream.
     *
     * @param compressorOutputStream the DeflateCompressorOutputStream to write compressed data to
     */
    public DeflateCompressor(DeflateCompressorOutputStream compressorOutputStream) {
        super(compressorOutputStream);
    }

    /**
     * Constructor that initializes the DeflateCompressor with a DeflateCompressorBuilder.
     *
     * @param builder the DeflateCompressorBuilder to build from
     * @throws IOException if an I/O error occurs while creating the compressor output stream
     */
    public DeflateCompressor(DeflateCompressorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Creates a DeflateDecompressorBuilder using the provided OutputStream.
     *
     * @param compressorOutputStream the OutputStream to write compressed data to
     * @return a new DeflateCompressorBuilder
     */
    public static DeflateCompressorBuilder builder(OutputStream compressorOutputStream) {
        return new DeflateCompressorBuilder(compressorOutputStream);
    }

    /**
     * Creates a DeflateCompressorBuilder using the provided Path.
     *
     * @param path the Path to write compressed data to
     * @return a new DeflateCompressorBuilder
     * @throws IOException if an I/O error occurs while creating the output stream
     */
    public static DeflateCompressorBuilder builder(Path path) throws IOException {
        return new DeflateCompressorBuilder(path);
    }

    /**
     * Builder for creating a {@link DeflateCompressorOutputStream}.
     *
     * @param <P> the type of the parent builder
     * @since 2.2
     */
    public static class DeflateOutputStreamBuilder<P> {
        private final P parent;
        private final OutputStream outputStream;
        private boolean zlibHeader = true;
        private int compressionLevel = Deflater.DEFAULT_COMPRESSION;

        /**
         * Constructor that initializes the DeflateOutputStreamBuilder with a parent builder and an OutputStream.
         *
         * @param parent the parent builder instance
         * @param outputStream the OutputStream to write compressed data to
         */
        public DeflateOutputStreamBuilder(P parent, OutputStream outputStream) {
            this.parent = parent;
            this.outputStream = outputStream;
        }

        /**
         * Sets the compression level for the Deflate output stream.
         *
         * @param compressionLevel the desired compression level (0-9)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if the compression level is not between 0 and 9
         */
        public DeflateOutputStreamBuilder<P> setCompressionLevel(DeflateCompressionLevel compressionLevel) {
            if (compressionLevel.getValue() < 0 || compressionLevel.getValue() > 9) {
                throw new IllegalArgumentException("Invalid Deflate compression level: " + compressionLevel);
            }
            this.compressionLevel = compressionLevel.getValue();
            return this;
        }

        /**
         * Sets whether to include the Zlib header in the output stream.
         *
         * @param zlibHeader true to include the Zlib header, false otherwise
         * @return this builder instance for method chaining
         */
        public DeflateOutputStreamBuilder<P> setZlibHeader(boolean zlibHeader) {
            this.zlibHeader = zlibHeader;
            return this;
        }

        /**
         * Builds a {@link DeflateCompressorOutputStream} with the current configuration.
         *
         * @return a new DeflateCompressorOutputStream instance
         */
        public DeflateCompressorOutputStream buildOutputStream() {
            DeflateParameters deflateParameters = new DeflateParameters();
            if (compressionLevel != Deflater.DEFAULT_COMPRESSION) {
                deflateParameters.setCompressionLevel(compressionLevel);
            }
            deflateParameters.setWithZlibHeader(zlibHeader);
            return new DeflateCompressorOutputStream(outputStream, deflateParameters);
        }

        /**
         * Returns the parent builder.
         *
         * @return the parent builder instance
         */
        public P parentBuilder() {
            return parent;
        }
    }

    /**
     * Builder for creating a {@link DeflateCompressor}.
     *
     * @since 2.2
     */
    public static class DeflateCompressorBuilder
            extends CompressorBuilder<DeflateCompressorOutputStream, DeflateCompressorBuilder, DeflateCompressor> {
        private final DeflateOutputStreamBuilder<DeflateCompressorBuilder> compressorOutputStreamBuilder;

        /**
         * Constructor that initializes the DeflateCompressorBuilder with an OutputStream.
         *
         * @param path the path to write compressed data to
         * @throws IOException if an I/O error occurs while creating the output stream
         */
        public DeflateCompressorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Constructor that initializes the DeflateCompressorBuilder with an OutputStream.
         *
         * @param outputStream the OutputStream to write compressed data to
         */
        public DeflateCompressorBuilder(OutputStream outputStream) {
            super(outputStream);
            this.compressorOutputStreamBuilder = new DeflateOutputStreamBuilder<>(this, outputStream);
        }

        /**
         * Returns the DeflateOutputStreamBuilder associated with this compressor builder.
         *
         * @return the DeflateOutputStreamBuilder instance
         */
        public DeflateOutputStreamBuilder<DeflateCompressorBuilder> compressorOutputStreamBuilder() {
            return compressorOutputStreamBuilder;
        }

        /**
         * Sets the compression level for the Deflate compressor.
         *
         * @return this builder instance for method chaining
         */
        @Override
        protected DeflateCompressorBuilder getThis() {
            return this;
        }

        /**
         * Builds a {@link DeflateCompressorOutputStream} using the current configuration.
         *
         * @return a new DeflateCompressorOutputStream instance
         * @throws IOException if an I/O error occurs while creating the compressor output stream
         */
        @Override
        public DeflateCompressorOutputStream buildCompressorOutputStream() throws IOException {
            return compressorOutputStreamBuilder.buildOutputStream();
        }

        /**
         * Builds a {@link DeflateCompressor} using the current configuration.
         *
         * @return a new DeflateCompressor instance
         * @throws IOException if an I/O error occurs while creating the compressor output stream
         */
        @Override
        public DeflateCompressor build() throws IOException {
            return new DeflateCompressor(this);
        }
    }
}
