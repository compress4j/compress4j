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
package io.github.compress4j.archivers.ar;

import io.github.compress4j.archivers.ArchiveExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    protected Entry nextEntry() throws IOException {
        ArArchiveEntry ae = getNextArArchiveEntry();
        if (ae == null) return null;

        // AR format only supports regular files
        return new Entry(ae.getName(), false, ae.getSize());
    }

    /** {@inheritDoc} */
    @Override
    protected InputStream openEntryStream(Entry entry) {
        return archiveInputStream;
    }

    /**
     * Get the next {@code ArArchiveEntry} from the {@code ArArchiveInputStream}.
     *
     * @return the next {@code ArArchiveEntry}
     * @throws IOException – if the next entry could not be read
     */
    private ArArchiveEntry getNextArArchiveEntry() throws IOException {
        return archiveInputStream.getNextEntry();
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
