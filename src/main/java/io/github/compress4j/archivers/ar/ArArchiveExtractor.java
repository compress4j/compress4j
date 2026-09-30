/*
 * Copyright 2025-2026 The Compress4J Project
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
package io.github.compress4j.archivers.ar;

import io.github.compress4j.archivers.ArchiveExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;

/**
 * The AR archive extractor.
 *
 * @since 2.2
 */
public class ArArchiveExtractor extends ArchiveExtractor<ArArchiveInputStream> {

    /**
     * Create a new ArArchiveExtractor with the given input stream.
     *
     * @param arArchiveInputStream the input AR Archive Input Stream
     */
    public ArArchiveExtractor(ArArchiveInputStream arArchiveInputStream) {
        super(arArchiveInputStream);
    }

    /**
     * Create a new ArArchiveExtractor with the given input stream and options.
     *
     * @param builder the archive input stream builder
     * @throws IOException if an I/O error occurred
     */
    public ArArchiveExtractor(ArArchiveExtractorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link ArArchiveExtractorBuilder}
     *
     * @param path the path to read the archive from
     * @return An instance of the {@link ArArchiveExtractorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static ArArchiveExtractorBuilder builder(Path path) throws IOException {
        return new ArArchiveExtractorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link ArArchiveExtractorBuilder}
     *
     * @param inputStream the input stream to read the archive from
     * @return An instance of the {@link ArArchiveExtractorBuilder}
     */
    public static ArArchiveExtractorBuilder builder(InputStream inputStream) {
        return new ArArchiveExtractorBuilder(inputStream);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Entry> nextEntry() throws IOException {
        ArArchiveEntry ae = archiveInputStream.getNextEntry();
        if (ae == null) return Optional.empty();

        int mode = ae.getMode();
        if ((mode & ArArchiveCreator.S_IFMT) == ArArchiveCreator.S_IFLNK) {
            return Optional.of(new Entry(ae.getName(), Entry.Type.SYMLINK, mode, readSymlinkTargetStoredAsContent(ae)));
        }
        return Optional.of(new Entry(ae.getName(), Entry.Type.FILE, mode));
    }

    private String readSymlinkTargetStoredAsContent(ArArchiveEntry entry) throws IOException {
        byte[] targetBytes = readEntryContent(entry.getName(), archiveInputStream, entry.getSize());
        return new String(targetBytes, StandardCharsets.UTF_8);
    }

    /** {@inheritDoc} */
    @Override
    public InputStream openEntryStream(Entry entry) {
        return archiveInputStream;
    }

    /** AR archive extractor builder */
    public static class ArArchiveExtractorBuilder
            extends ArchiveExtractorBuilder<ArArchiveInputStream, ArArchiveExtractorBuilder, ArArchiveExtractor> {

        /** Input stream to read from for extraction. */
        private final InputStream inputStream;

        /**
         * Create a new {@link ArArchiveExtractor} with the given path.
         *
         * @param path the path to read the archive from
         * @throws IOException if an I/O error occurred
         */
        public ArArchiveExtractorBuilder(Path path) throws IOException {
            this(Files.newInputStream(path));
        }

        /**
         * Create a new {@link ArArchiveExtractor} with the given input stream.
         *
         * @param inputStream the input stream
         */
        public ArArchiveExtractorBuilder(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        /** {@inheritDoc} */
        @Override
        protected ArArchiveExtractorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ArArchiveInputStream buildArchiveInputStream() {
            return new ArArchiveInputStream(inputStream);
        }

        /** {@inheritDoc} */
        @Override
        public ArArchiveExtractor build() throws IOException {
            return new ArArchiveExtractor(this);
        }
    }
}
