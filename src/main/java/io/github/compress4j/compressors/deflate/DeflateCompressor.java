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

public class DeflateCompressor extends Compressor<DeflateCompressorOutputStream> {

    protected DeflateCompressor(DeflateCompressorOutputStream compressorOutputStream) {
        super(compressorOutputStream);
    }

    public DeflateCompressor(DeflateCompressorBuilder builder) throws IOException {
        super(builder);
    }

    public static DeflateCompressorBuilder builder(OutputStream compressorOutputStream) {
        return new DeflateCompressorBuilder(compressorOutputStream);
    }

    public static DeflateCompressorBuilder builder(Path path) throws IOException {
        return new DeflateCompressorBuilder(path);
    }

    public static class DeflateOutputStreamBuilder<P> {
        private final P parent;
        private final OutputStream outputStream;
        private boolean zlibHeader = true;
        private int compressionLevel = Deflater.DEFAULT_COMPRESSION;

        public DeflateOutputStreamBuilder(P parent, OutputStream outputStream) {
            this.parent = parent;
            this.outputStream = outputStream;
        }

        public DeflateOutputStreamBuilder<P> setCompressionLevel(DeflateCompressionLevel compressionLevel) {
            if (compressionLevel.getValue() < 0 || compressionLevel.getValue() > 9) {
                throw new IllegalArgumentException("Invalid Deflate compression level: " + compressionLevel);
            }
            this.compressionLevel = compressionLevel.getValue();
            return this;
        }

        public DeflateOutputStreamBuilder<P> setZlibHeader(boolean zlibHeader) {
            this.zlibHeader = zlibHeader;
            return this;
        }

        public DeflateCompressorOutputStream buildOutputStream() {
            DeflateParameters deflateParameters = new DeflateParameters();
            if (compressionLevel != Deflater.DEFAULT_COMPRESSION) {
                deflateParameters.setCompressionLevel(compressionLevel);
            }
            deflateParameters.setWithZlibHeader(zlibHeader);
            return new DeflateCompressorOutputStream(outputStream, deflateParameters);
        }

        public P parentBuilder() {
            return parent;
        }
    }

    public static class DeflateCompressorBuilder
            extends CompressorBuilder<DeflateCompressorOutputStream, DeflateCompressorBuilder, DeflateCompressor> {
        private final DeflateOutputStreamBuilder<DeflateCompressorBuilder> compressorOutputStreamBuilder;

        public DeflateCompressorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        protected DeflateCompressorBuilder(OutputStream outputStream) {
            super(outputStream);
            this.compressorOutputStreamBuilder = new DeflateOutputStreamBuilder<>(this, outputStream);
        }

        public DeflateOutputStreamBuilder<DeflateCompressorBuilder> compressorOutputStreamBuilder() {
            return compressorOutputStreamBuilder;
        }

        @Override
        protected DeflateCompressorBuilder getThis() {
            return this;
        }

        @Override
        public DeflateCompressorOutputStream buildCompressorOutputStream() throws IOException {
            return compressorOutputStreamBuilder.buildOutputStream();
        }

        @Override
        public DeflateCompressor build() throws IOException {
            return new DeflateCompressor(this);
        }
    }
}
