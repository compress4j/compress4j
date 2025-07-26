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
package io.github.compress4j.compressors.gzip;

import static java.nio.file.Files.newInputStream;

import io.github.compress4j.compressors.Decompressor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * This class provides a GZip decompressor that reads from a GzipCompressorInputStream. It extends the Decompressor
 * class and provides a builder for creating instances.
 */
public class GzipDecompressor extends Decompressor<GzipCompressorInputStream> {

    /**
     * Constructor that takes a GzipDecompressorBuilder.
     *
     * @param builder the GzipDecompressorBuilder to build from.
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    public GzipDecompressor(GzipDecompressorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Constructor that takes a GzipCompressorInputStream.
     *
     * @param compressorInputStream the GzipCompressorInputStream to read from.
     */
    public GzipDecompressor(GzipCompressorInputStream compressorInputStream) {
        super(compressorInputStream);
    }

    public static GzipDecompressorBuilder builder(Path path) throws IOException {
        return new GzipDecompressorBuilder(Files.newInputStream(path));
    }

    public static GzipDecompressorBuilder builder(GzipCompressorInputStream inputStream) {
        return new GzipDecompressorBuilder(inputStream);
    }

    public static class GzipDecompressorInputStreamBuilder {
        private final GzipDecompressorBuilder parent;
        private final InputStream inputStream;
        private boolean decompressConcatenated = false;

        public GzipDecompressorInputStreamBuilder(GzipDecompressorBuilder parent, InputStream inputStream) {
            this.parent = parent;
            this.inputStream = inputStream;
        }

        @SuppressWarnings("UnusedReturnValue")
        public GzipDecompressorInputStreamBuilder setDecompressConcatenated(boolean decompressConcatenated) {
            this.decompressConcatenated = decompressConcatenated;
            return this;
        }

        public GzipCompressorInputStream buildInputStream() throws IOException {
            return new GzipCompressorInputStream(inputStream, decompressConcatenated);
        }

        public GzipDecompressorBuilder parentBuilder() {
            return parent;
        }
    }

    public static class GzipDecompressorBuilder
            extends Decompressor.DecompressorBuilder<
                    GzipCompressorInputStream, GzipDecompressor, GzipDecompressorBuilder> {

        private final GzipDecompressorInputStreamBuilder inputStreamBuilder;
        /**
         * Constructor that takes a GzipCompressorInputStream.
         *
         * @param inputStream the GzipCompressorInputStream to read from.
         */
        public GzipDecompressorBuilder(InputStream inputStream) {
            super(inputStream);
            this.inputStreamBuilder = new GzipDecompressorInputStreamBuilder(this, inputStream);
        }

        public GzipDecompressorBuilder(Path path) throws IOException {
            this(newInputStream(path));
        }

        public GzipDecompressorBuilder(File file) throws IOException {
            this(file.toPath());
        }

        public GzipDecompressorInputStreamBuilder inputStreamBuilder() {
            return inputStreamBuilder;
        }

        @Override
        public GzipCompressorInputStream buildCompressorInputStream() throws IOException {
            return inputStreamBuilder.buildInputStream();
        }

        @Override
        protected GzipDecompressorBuilder getThis() {
            return this;
        }

        @Override
        public GzipDecompressor build() throws IOException {

            return new GzipDecompressor(this);
        }
    }
}
