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

public class BZip2Compressor extends Compressor<BZip2CompressorOutputStream> {
    protected BZip2Compressor(BZip2CompressorOutputStream compressorOutputStream) {
        super(compressorOutputStream);
    }

    protected BZip2Compressor(BZip2CompressorBuilder builder) throws IOException {
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

    public static class BZip2CompressorOutputStreamBuilder<P> {
        protected final OutputStream outputStream;
        private final P parent;
        private int blockSize = MAX_BLOCKSIZE;

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

        public BZip2CompressorOutputStream build() throws IOException {
            return new BZip2CompressorOutputStream(outputStream, blockSize);
        }

        public P parentBuilder() {
            return parent;
        }
    }

    public static class BZip2CompressorBuilder
            extends CompressorBuilder<BZip2CompressorOutputStream, BZip2CompressorBuilder, BZip2Compressor> {

        private final BZip2CompressorOutputStreamBuilder<BZip2CompressorBuilder> compressorOutputStreamBuilder;

        /**
         * Create a new {@link BZip2CompressorBuilder} with the given path.
         *
         * @param path the path to write the compressor to
         * @throws IOException if an I/O error occurred
         */
        protected BZip2CompressorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Create a new {@link CompressorBuilder} with the given output stream.
         *
         * @param outputStream the output stream
         */
        protected BZip2CompressorBuilder(OutputStream outputStream) {
            super(outputStream);
            this.compressorOutputStreamBuilder = new BZip2CompressorOutputStreamBuilder<>(this, outputStream);
        }

        public BZip2CompressorOutputStreamBuilder<BZip2CompressorBuilder> compressorOutputStreamBuilder() {
            return compressorOutputStreamBuilder;
        }

        @Override
        protected BZip2CompressorBuilder getThis() {
            return this;
        }

        @Override
        public BZip2CompressorOutputStream buildCompressorOutputStream() throws IOException {
            return compressorOutputStreamBuilder.build();
        }

        @Override
        public BZip2Compressor build() throws IOException {
            return new BZip2Compressor(this);
        }
    }
}
