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
package io.github.compress4j.compressors.deflate;

import static java.nio.file.Files.newInputStream;

import io.github.compress4j.compressors.Decompressor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateParameters;

public class DeflateDecompressor extends Decompressor<DeflateCompressorInputStream> {

    protected DeflateDecompressor(DeflateCompressorInputStream compressorInputStream) {
        super(compressorInputStream);
    }

    protected DeflateDecompressor(DeflateDecompressorBuilder builder) throws IOException {
        super(builder);
    }

    public static DeflateDecompressorBuilder builder(InputStream inputStream) {
        return new DeflateDecompressorBuilder(inputStream);
    }

    public static DeflateDecompressorBuilder builder(Path path) throws IOException {
        return new DeflateDecompressorBuilder(Files.newInputStream(path));
    }

    public static class DeflateDecompressorInputStreamBuilder {
        private final DeflateDecompressorBuilder parent;
        private final InputStream inputStream;
        private boolean withZlibHeader = true;

        public DeflateDecompressorInputStreamBuilder(DeflateDecompressorBuilder parent, InputStream inputStream) {
            this.parent = parent;
            this.inputStream = inputStream;
        }

        @SuppressWarnings("UnusedReturnValue")
        public DeflateDecompressorInputStreamBuilder setWithZlibHeader(boolean withZlibHeader) {
            this.withZlibHeader = withZlibHeader;
            return this;
        }

        public DeflateCompressorInputStream buildInputStream() {
            DeflateParameters parameters = new DeflateParameters();
            parameters.setWithZlibHeader(withZlibHeader);

            return new DeflateCompressorInputStream(inputStream, parameters);
        }

        public DeflateDecompressorBuilder parentBuilder() {
            return parent;
        }
    }

    public static class DeflateDecompressorBuilder
            extends Decompressor.DecompressorBuilder<
                    DeflateCompressorInputStream, DeflateDecompressor, DeflateDecompressorBuilder> {

        private final DeflateDecompressorInputStreamBuilder inputStreamBuilder;

        protected DeflateDecompressorBuilder(InputStream inputStream) {
            super(inputStream);
            this.inputStreamBuilder = new DeflateDecompressorInputStreamBuilder(this, inputStream);
        }

        public DeflateDecompressorBuilder(Path path) throws IOException {
            this(newInputStream(path));
        }

        public DeflateDecompressorBuilder(File file) throws IOException {
            this(file.toPath());
        }

        public DeflateDecompressorInputStreamBuilder inputStreamBuilder() {
            return inputStreamBuilder;
        }

        @Override
        public DeflateCompressorInputStream buildCompressorInputStream() {
            return inputStreamBuilder.buildInputStream();
        }

        @Override
        protected DeflateDecompressorBuilder getThis() {
            return this;
        }

        @Override
        public DeflateDecompressor build() throws IOException {
            return new DeflateDecompressor(this);
        }
    }
}
