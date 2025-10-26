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
package io.github.compress4j.archivers.cpio;

import io.github.compress4j.archivers.ArchiveExtractor;
import io.github.compress4j.archivers.ArchiveExtractor.ArchiveExtractorBuilder;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;

/**
 * The CPIO archive extractor.
 *
 * @since 2.2
 */
public class CpioArchiveExtractor extends ArchiveExtractor<CpioArchiveInputStream> {

    /**
     * Create a new CpioArchiveExtractor with the given input stream.
     *
     * @param cpioArchiveInputStream the input CPIO Archive Input Stream
     */
    public CpioArchiveExtractor(CpioArchiveInputStream cpioArchiveInputStream) {
        super(cpioArchiveInputStream);
    }

    /**
     * Create a new CpioArchiveExtractor with the given input stream and options.
     *
     * @param builder the archive input stream builder
     * @throws IOException if an I/O error occurred
     */
    public CpioArchiveExtractor(CpioArchiveExtractorBuilder builder) throws IOException {
        super(builder);
    }

    @Override
    protected InputStream openEntryStream(Entry entry) {
        return archiveInputStream;
    }

    @Override
    protected @Nullable Entry nextEntry() throws IOException {
        CpioArchiveEntry cpioEntry = archiveInputStream.getNextEntry();
        if (cpioEntry == null || "TRAILER!!!".equals(cpioEntry.getName())) {
            return null;
        }

        if (cpioEntry.isDirectory()) {
            return new Entry(cpioEntry.getName(), true, cpioEntry.getSize());
        } else {
            return new Entry(cpioEntry.getName(), false, cpioEntry.getSize());
        }
    }

    /**
     * Helper static method to create an instance of the {@link CpioArchiveExtractorBuilder}
     *
     * @param path the path to read the archive from
     * @return a new instance of {@link CpioArchiveExtractorBuilder}
     * @throws IOException if an I/O error occurs opening the file
     */
    public static CpioArchiveExtractorBuilder builder(Path path) throws IOException {
        return new CpioArchiveExtractorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link CpioArchiveExtractorBuilder}
     *
     * @param file the file to read the archive from
     * @return a new instance of {@link CpioArchiveExtractorBuilder}
     * @throws IOException if an I/O error occurs opening the file
     */
    public static CpioArchiveExtractorBuilder builder(File file) throws IOException {
        return builder(file.toPath());
    }

    /**
     * Helper static method to create an instance of the {@link CpioArchiveExtractorBuilder}
     *
     * @param inputStream the input stream to read the archive from
     * @return a new instance of {@link CpioArchiveExtractorBuilder}
     */
    public static CpioArchiveExtractorBuilder builder(InputStream inputStream) {
        return new CpioArchiveExtractorBuilder(inputStream);
    }

    /**
     * Builder for configuring and creating a {@link CpioArchiveInputStream}.
     *
     * @param <P> the type of the parent builder
     * @since 2.2
     */
    public static class CpioArchiveInputStreamBuilder<P> {
        /** The input stream to read the archive from. */
        protected final InputStream inputStream;

        private final P parent;
        private int blockSize = 512;
        private String encoding = "UTF-8";

        /**
         * Constructs a builder for a CPIO input stream.
         *
         * @param parent the parent builder
         * @param inputStream the input stream to read the archive from
         */
        public CpioArchiveInputStreamBuilder(P parent, InputStream inputStream) {
            this.parent = parent;
            this.inputStream = inputStream;
        }

        /**
         * Sets the block size for reading the CPIO archive.
         *
         * @param blockSize the block size in bytes
         * @return this builder instance
         */
        public CpioArchiveInputStreamBuilder<P> blockSize(int blockSize) {
            this.blockSize = blockSize;
            return this;
        }

        /**
         * Sets the character encoding for file names.
         *
         * @param encoding the character encoding
         * @return this builder instance
         */
        public CpioArchiveInputStreamBuilder<P> encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        /**
         * Returns the parent builder.
         *
         * @return the parent builder
         */
        public P and() {
            return parent;
        }

        /**
         * Builds the {@link CpioArchiveInputStream} with the configured options.
         *
         * @return a new CPIO archive input stream
         * @throws IOException if an I/O error occurs during stream creation
         */
        public CpioArchiveInputStream build() throws IOException {
            return new CpioArchiveInputStream(inputStream, blockSize, encoding);
        }
    }

    /**
     * Builder for configuring and creating {@link CpioArchiveExtractor} instances.
     *
     * @since 2.2
     */
    public static class CpioArchiveExtractorBuilder
            extends ArchiveExtractorBuilder<CpioArchiveInputStream, CpioArchiveExtractorBuilder, CpioArchiveExtractor> {

        private final CpioArchiveInputStreamBuilder<CpioArchiveExtractorBuilder> cpioInputStreamBuilder;

        /**
         * Constructs a CpioArchiveExtractorBuilder with the given file path.
         *
         * @param path the file path to read the archive from
         * @throws IOException if an I/O error occurs opening the file
         */
        public CpioArchiveExtractorBuilder(Path path) throws IOException {
            this(Files.newInputStream(path));
        }

        /**
         * Constructs a CpioArchiveExtractorBuilder with the given input stream.
         *
         * @param inputStream the input stream to read the archive from
         */
        public CpioArchiveExtractorBuilder(InputStream inputStream) {
            this.cpioInputStreamBuilder = new CpioArchiveInputStreamBuilder<>(this, inputStream);
        }

        /**
         * Access the CPIO input stream builder for configuration.
         *
         * @return the CPIO input stream builder
         */
        public CpioArchiveInputStreamBuilder<CpioArchiveExtractorBuilder> cpioInputStream() {
            return cpioInputStreamBuilder;
        }

        @Override
        protected CpioArchiveExtractorBuilder getThis() {
            return this;
        }

        @Override
        public CpioArchiveInputStream buildArchiveInputStream() throws IOException {
            return cpioInputStreamBuilder.build();
        }

        @Override
        public CpioArchiveExtractor build() throws IOException {
            return new CpioArchiveExtractor(this);
        }
    }
}
