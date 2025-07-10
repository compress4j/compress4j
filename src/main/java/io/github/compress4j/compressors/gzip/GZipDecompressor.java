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

import io.github.compress4j.compressors.Decompressor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import static java.nio.file.Files.newInputStream;

/**
 * This class provides a GZip decompressor that reads from a GzipCompressorInputStream. It extends the Decompressor
 * class and provides a builder for creating instances.
 */
public class GZipDecompressor extends Decompressor<GzipCompressorInputStream> {

    /**
     * Constructor that takes a GZipDecompressorBuilder.
     *
     * @param builder the GZipDecompressorBuilder to build from.
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    public GZipDecompressor(GZipDecompressorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Constructor that takes a GzipCompressorInputStream.
     *
     * @param compressorInputStream the GzipCompressorInputStream to read from.
     */
    public GZipDecompressor(GzipCompressorInputStream compressorInputStream) {
        super(compressorInputStream);
    }

    public static GZipDecompressorBuilder builder(Path path) throws IOException {
        return new GZipDecompressorBuilder(new GzipCompressorInputStream(Files.newInputStream(path)));
    }

    public static GZipDecompressorBuilder builder(GzipCompressorInputStream inputStream) {
        return new GZipDecompressorBuilder(inputStream);
    }

    public static class GZipDecompressorInputStreamBuilder {
        private final GZipDecompressorBuilder parent;
        private final InputStream inputStream;
        private boolean decompressConcatenated = false;

        public GZipDecompressorInputStreamBuilder(GZipDecompressorBuilder parent, InputStream inputStream) {
            this.parent = parent;
            this.inputStream = inputStream;
        }

        public GZipDecompressorInputStreamBuilder setDecompressConcatenated(boolean decompressConcatenated) {
            this.decompressConcatenated = decompressConcatenated;
            return this;
        }

        public GzipCompressorInputStream buildInputStream() throws IOException {
            return new GzipCompressorInputStream(inputStream, decompressConcatenated);
        }

        public GZipDecompressorBuilder parentBuilder() {
            return parent;
        }
    }

    public static class GZipDecompressorBuilder
            extends Decompressor.DecompressorBuilder<
                    GzipCompressorInputStream, GZipDecompressor, GZipDecompressorBuilder> {

        private final GZipDecompressorInputStreamBuilder inputStreamBuilder;
        /**
         * Constructor that takes a GzipCompressorInputStream.
         *
         * @param inputStream the GzipCompressorInputStream to read from.
         */
        public GZipDecompressorBuilder(InputStream inputStream) {
            super(inputStream);
            this.inputStreamBuilder = new GZipDecompressorInputStreamBuilder(this, inputStream);
        }

        public GZipDecompressorBuilder(Path path) throws IOException {
            this(newInputStream(path));
        }

        public GZipDecompressorBuilder(File file) throws IOException {
            this(file.toPath());
        } // todo is it bad practice to call another constructer which would then invoke another constructer?


        public GZipDecompressorInputStreamBuilder inputStreamBuilder() {
            return inputStreamBuilder;
        }

        @Override
        public GzipCompressorInputStream buildCompressorInputStream() throws IOException {
            return inputStreamBuilder.buildInputStream();
        }

        @Override
        protected GZipDecompressor.GZipDecompressorBuilder getThis() {
            return this;
        }

        @Override
        public GZipDecompressor build() throws IOException {

            return new GZipDecompressor(this);
        }
    }
}
