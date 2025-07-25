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
package io.github.compress4j.compressors.bzip2;

import static java.nio.file.Files.newInputStream;

import io.github.compress4j.compressors.Decompressor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * This class provides a BZip2 decompressor that reads from a BZip2CompressorInputStream. It extends the Decompressor
 * class and provides a builder for creating instances.
 */
public class BZip2Decompressor extends Decompressor<BZip2CompressorInputStream> {

    /**
     * Constructor that takes a BZip2CompressorInputStream.
     *
     * @param inputStream the BZip2CompressorInputStream to read from.
     */
    public BZip2Decompressor(BZip2CompressorInputStream inputStream) {
        super(inputStream);
    }

    /**
     * Constructor that takes a BZip2DecompressorBuilder.
     *
     * @param builder the BZip2DecompressorBuilder to build from.
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    public BZip2Decompressor(BZip2DecompressorBuilder builder) throws IOException {
        super(builder);
    }

    public static BZip2DecompressorBuilder builder(InputStream inputStream) {
        return new BZip2DecompressorBuilder(inputStream);
    }

    public static BZip2DecompressorBuilder builder(Path path) throws IOException {
        return new BZip2DecompressorBuilder(newInputStream(path));
    }

    public static class BZip2DecompressorInputStreamBuilder<P> {
        private final P parent;
        private final InputStream inputStream;
        private boolean decompressConcatenated = false;

        public BZip2DecompressorInputStreamBuilder(P parent, InputStream inputStream) {
            this.parent = parent;
            this.inputStream = inputStream;
        }

        public BZip2DecompressorInputStreamBuilder<P> setDecompressConcatenated(boolean decompressConcatenated) {
            this.decompressConcatenated = decompressConcatenated;
            return this;
        }

        public BZip2CompressorInputStream buildInputStream() throws IOException {
            return new BZip2CompressorInputStream(inputStream, decompressConcatenated);
        }

        public P parentBuilder() {
            return parent;
        }
    }

    public static class BZip2DecompressorBuilder
            extends Decompressor.DecompressorBuilder<
                    BZip2CompressorInputStream, BZip2Decompressor, BZip2DecompressorBuilder> {

        private final BZip2DecompressorInputStreamBuilder<BZip2DecompressorBuilder> inputStreamBuilder;
        /**
         * Constructor that takes a InputStream.
         *
         * @param inputStream the InputStream to read from.
         */
        public BZip2DecompressorBuilder(InputStream inputStream) {
            super(inputStream);
            this.inputStreamBuilder = new BZip2DecompressorInputStreamBuilder<>(this, inputStream);
        }

        public BZip2DecompressorBuilder(Path path) throws IOException {
            this(newInputStream(path));
        }

        public BZip2DecompressorBuilder(File file) throws IOException {
            this(file.toPath());
        }

        public BZip2DecompressorInputStreamBuilder<BZip2DecompressorBuilder> inputStreamBuilder() {
            return inputStreamBuilder;
        }

        @Override
        public BZip2CompressorInputStream buildCompressorInputStream() throws IOException {
            return inputStreamBuilder.buildInputStream();
        }

        @Override
        protected BZip2DecompressorBuilder getThis() {
            return this;
        }

        @Override
        public BZip2Decompressor build() throws IOException {
            return new BZip2Decompressor(this);
        }
    }
}
