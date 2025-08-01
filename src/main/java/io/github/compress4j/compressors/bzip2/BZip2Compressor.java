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
package io.github.compress4j.compressors.bzip2;

import static org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream.MAX_BLOCKSIZE;

import io.github.compress4j.compressors.Compressor;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * This class provides a BZip2 compressor that writes to a BZip2CompressorOutputStream. It extends the Compressor class
 * and provides a builder for creating instances.
 */
public class BZip2Compressor extends Compressor<BZip2CompressorOutputStream> {
    /**
     * Constructor that takes a BZip2CompressorOutputStream.
     *
     * @param compressorOutputStream the BZip2CompressorOutputStream to write to.
     */
    public BZip2Compressor(BZip2CompressorOutputStream compressorOutputStream) {
        super(compressorOutputStream);
    }

    /**
     * Constructor that takes a BZip2CompressorBuilder.
     *
     * @param builder the BZip2CompressorBuilder to build from.
     * @throws IOException if an I/O error occurred
     */
    public BZip2Compressor(BZip2CompressorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link BZip2CompressorBuilder}
     *
     * @param path the path to write the compressor to
     * @return An instance of the {@link BZip2CompressorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static BZip2CompressorBuilder builder(Path path) throws IOException {
        return new BZip2CompressorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link BZip2CompressorBuilder}
     *
     * @param outputStream the output stream
     * @return An instance of the {@link BZip2CompressorBuilder}
     */
    public static BZip2CompressorBuilder builder(OutputStream outputStream) {
        return new BZip2CompressorBuilder(outputStream);
    }

    /**
     * Builder class for creating a {@link BZip2CompressorOutputStream}.
     *
     * @param <P> The type of the parent builder.
     */
    public static class BZip2CompressorOutputStreamBuilder<P> {
        private final P parent;
        private int blockSize = MAX_BLOCKSIZE;

        /** The output stream to write to. */
        protected final OutputStream outputStream;

        /**
         * Create a new {@link BZip2CompressorOutputStreamBuilder} with the given parent and output stream.
         *
         * @param parent the parent builder
         * @param outputStream the output stream to write to
         */
        public BZip2CompressorOutputStreamBuilder(P parent, OutputStream outputStream) {
            this.parent = parent;
            this.outputStream = outputStream;
        }

        /**
         * Set blockSize. Always: in the range 0..9, the current block size is 100000 * this number.
         *
         * @param blockSize the blockSize as 100k units.
         * @return the instance of the {@link BZip2CompressorBuilder}
         */
        public BZip2CompressorOutputStreamBuilder<P> blockSize(int blockSize) {
            if (blockSize < 1) {
                throw new IllegalArgumentException("blockSize(" + blockSize + ") < 1");
            }
            if (blockSize > 9) {
                throw new IllegalArgumentException("blockSize(" + blockSize + ") > 9");
            }
            this.blockSize = blockSize;
            return this;
        }

        /**
         * Builds the {@link BZip2CompressorOutputStream} with the configured parameters.
         *
         * @return the {@link BZip2CompressorOutputStream} instance
         * @throws IOException if an I/O error occurred
         */
        public BZip2CompressorOutputStream build() throws IOException {
            return new BZip2CompressorOutputStream(outputStream, blockSize);
        }

        /**
         * Returns the parent builder.
         *
         * @return the parent builder
         */
        public P parentBuilder() {
            return parent;
        }
    }

    /** Builder class for creating a {@link BZip2Compressor}. */
    public static class BZip2CompressorBuilder
            extends CompressorBuilder<BZip2CompressorOutputStream, BZip2CompressorBuilder, BZip2Compressor> {

        private final BZip2CompressorOutputStreamBuilder<BZip2CompressorBuilder> compressorOutputStreamBuilder;

        /**
         * Create a new {@link BZip2CompressorBuilder} with the given path.
         *
         * @param path the path to write the compressor to
         * @throws IOException if an I/O error occurred
         */
        public BZip2CompressorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Create a new {@link CompressorBuilder} with the given output stream.
         *
         * @param outputStream the output stream
         */
        public BZip2CompressorBuilder(OutputStream outputStream) {
            super(outputStream);
            this.compressorOutputStreamBuilder = new BZip2CompressorOutputStreamBuilder<>(this, outputStream);
        }

        /**
         * Returns the BZip2CompressorOutputStreamBuilder for this compressor.
         *
         * @return the BZip2CompressorOutputStreamBuilder
         */
        public BZip2CompressorOutputStreamBuilder<BZip2CompressorBuilder> compressorOutputStreamBuilder() {
            return compressorOutputStreamBuilder;
        }

        /**
         * Sets the block size for the BZip2 compressor.
         *
         * @return this builder instance
         */
        @Override
        public BZip2CompressorBuilder getThis() {
            return this;
        }

        /**
         * Sets the block size for the BZip2 compressor.
         *
         * @return this builder instance
         */
        @Override
        public BZip2CompressorOutputStream buildCompressorOutputStream() throws IOException {
            return compressorOutputStreamBuilder.build();
        }

        /**
         * Builds the BZip2Compressor instance.
         *
         * @return a new BZip2Compressor instance
         * @throws IOException if an I/O error occurred
         */
        @Override
        public BZip2Compressor build() throws IOException {
            return new BZip2Compressor(this);
        }
    }
}
