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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.function.IOConsumer;

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

    /**
     * Creates a GzipDecompressorBuilder using the provided Path.
     *
     * @param path the Path to read from
     * @return a new GzipDecompressorBuilder
     * @throws IOException if an I/O error occurs while creating the input stream
     */
    public static GzipDecompressorBuilder builder(Path path) throws IOException {
        return new GzipDecompressorBuilder(Files.newInputStream(path));
    }

    /**
     * Creates a GzipDecompressorBuilder using the provided File.
     *
     * @param inputStream InputStream
     * @return a new GzipDecompressorBuilder
     */
    public static GzipDecompressorBuilder builder(InputStream inputStream) {
        return new GzipDecompressorBuilder(inputStream);
    }

    /** GzipDecompressorInputStream Builder */
    public static class GzipDecompressorInputStreamBuilder {
        private final GzipDecompressorBuilder parent;
        private final InputStream inputStream;

        /** True if decompressing multi-member streams. */
        private boolean decompressConcatenated;

        private Charset fileNameCharset = StandardCharsets.ISO_8859_1;

        private IOConsumer<GzipCompressorInputStream> onMemberStart;

        private IOConsumer<GzipCompressorInputStream> onMemberEnd;

        /**
         * Constructor that takes a parent GzipDecompressorBuilder and an InputStream.
         *
         * @param parent the parent GzipDecompressorBuilder
         * @param inputStream the InputStream to read from
         */
        public GzipDecompressorInputStreamBuilder(GzipDecompressorBuilder parent, InputStream inputStream) {
            this.parent = parent;
            this.inputStream = inputStream;
        }

        /**
         * Sets whether to decompress concatenated GZIP streams.
         *
         * @param decompressConcatenated true if concatenated streams should be decompressed, false otherwise
         * @return this instance
         */
        public GzipDecompressorInputStreamBuilder setDecompressConcatenated(boolean decompressConcatenated) {
            this.decompressConcatenated = decompressConcatenated;
            return this;
        }

        /**
         * Sets the Charset to use for writing file names and comments, where null maps to
         * {@link StandardCharsets.ISO_8859_1}.
         *
         * <p><em>Setting a value other than {@link StandardCharsets.ISO_8859_1} is not compliant with the <a
         * href="https://datatracker.ietf.org/doc/html/rfc1952">RFC 1952 GZIP File Format Specification</a></em>. Use at
         * your own risk of interoperability issues.
         *
         * <p>The default value is {@link StandardCharsets.ISO_8859_1}.
         *
         * @param fileNameCharset the Charset to use for writing file names and comments, null maps to
         *     {@link StandardCharsets.ISO_8859_1}.
         * @return this instance.
         */
        public GzipDecompressorInputStreamBuilder setFileNameCharset(final Charset fileNameCharset) {
            this.fileNameCharset = fileNameCharset;
            return this;
        }

        /**
         * Sets the consumer called when a member <em>trailer</em> is parsed.
         *
         * <p>When a member <em>header</em> is parsed, all {@link GzipParameters} values are initialized except
         * {@code trailerCrc} and {@code trailerISize}.
         *
         * <p>When a member <em>trailer</em> is parsed, the {@link GzipParameters} values {@code trailerCrc} and
         * {@code trailerISize} are set.
         *
         * @param onMemberEnd The consumer.
         * @return this instance.
         * @see GzipCompressorInputStream#getMetaData()
         */
        public GzipDecompressorInputStreamBuilder setOnMemberEnd(
                final IOConsumer<GzipCompressorInputStream> onMemberEnd) {
            this.onMemberEnd = onMemberEnd;
            return this;
        }

        /**
         * Sets the consumer called when a member <em>header</em> is parsed.
         *
         * <p>When a member <em>header</em> is parsed, all {@link GzipParameters} values are initialized except
         * {@code trailerCrc} and {@code trailerISize}.
         *
         * <p>When a member <em>trailer</em> is parsed, the {@link GzipParameters} values {@code trailerCrc} and
         * {@code trailerISize} are set.
         *
         * @param onMemberStart The consumer.
         * @return this instance.
         * @see GzipCompressorInputStream#getMetaData()
         */
        public GzipDecompressorInputStreamBuilder setOnMemberStart(
                final IOConsumer<GzipCompressorInputStream> onMemberStart) {
            this.onMemberStart = onMemberStart;
            return this;
        }

        /**
         * Builds a GzipCompressorInputStream using the provided InputStream and options.
         *
         * @return a new GzipCompressorInputStream
         * @throws IOException if an I/O error occurs while creating the stream
         */
        public GzipCompressorInputStream buildInputStream() throws IOException {
            return GzipCompressorInputStream.builder()
                    .setInputStream(inputStream)
                    .setDecompressConcatenated(decompressConcatenated)
                    .setFileNameCharset(fileNameCharset)
                    .setOnMemberStart(onMemberStart)
                    .setOnMemberEnd(onMemberEnd)
                    .get();
        }

        /**
         * Builds the GzipDecompressor using the parent builder.
         *
         * @return the parent GzipDecompressorBuilder
         */
        public GzipDecompressorBuilder parentBuilder() {
            return parent;
        }
    }

    /** Builder for creating instances of GzipDecompressor. */
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

        /**
         * Constructor that takes a Path to read from.
         *
         * @param path the Path to read from.
         * @throws IOException if an I/O error occurs while creating the input stream
         */
        public GzipDecompressorBuilder(Path path) throws IOException {
            this(newInputStream(path));
        }

        /**
         * Constructor that takes a File to read from.
         *
         * @param file the File to read from.
         * @throws IOException if an I/O error occurs while creating the input stream
         */
        public GzipDecompressorBuilder(File file) throws IOException {
            this(file.toPath());
        }

        /**
         * Constructor that takes a GzipCompressorInputStream.
         *
         * @return a new GzipDecompressorBuilder
         */
        public GzipDecompressorInputStreamBuilder compressorInputStreamBuilder() {
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
