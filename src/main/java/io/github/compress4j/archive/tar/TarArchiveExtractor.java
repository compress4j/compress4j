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
package io.github.compress4j.archive.tar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Tar ArchiveExtractor
 *
 * @since 2.2
 */
public class TarArchiveExtractor extends BaseTarArchiveExtractor {

    /**
     * Create a new TarArchiveExtractor with the given input stream.
     *
     * @param tarArchiveInputStream the input Tar Archive Input Stream
     */
    public TarArchiveExtractor(TarArchiveInputStream tarArchiveInputStream) {
        super(tarArchiveInputStream);
    }

    /**
     * Create a new TarArchiveExtractor with the given input stream and options.
     *
     * @param builder the archive input stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarArchiveExtractor(TarArchiveExtractorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link TarArchiveExtractorBuilder}
     *
     * @param path the path to write the archive to
     * @return An instance of the {@link TarArchiveExtractorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static TarArchiveExtractorBuilder builder(Path path) throws IOException {
        return new TarArchiveExtractorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link TarArchiveExtractorBuilder}
     *
     * @param inputStream the input stream to extract the archive to
     * @return An instance of the {@link TarArchiveExtractorBuilder}
     */
    public static TarArchiveExtractorBuilder builder(InputStream inputStream) {
        return new TarArchiveExtractorBuilder(inputStream);
    }

    /** Tar extractor builder */
    public static class TarArchiveExtractorBuilder
            extends BaseTarArchiveExtractorBuilder<TarArchiveExtractorBuilder, TarArchiveExtractor> {
        /**
         * Create a new {@link TarArchiveExtractor} with the given path.
         *
         * @param path the path to extract the archive to
         * @throws IOException if an I/O error occurred
         */
        public TarArchiveExtractorBuilder(Path path) throws IOException {
            this(Files.newInputStream(path));
        }

        /**
         * Create a new {@link TarArchiveExtractor} with the given input stream.
         *
         * @param inputStream the input stream
         */
        public TarArchiveExtractorBuilder(InputStream inputStream) {
            super(inputStream);
        }

        /** {@inheritDoc} */
        @Override
        protected TarArchiveExtractorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public TarArchiveInputStream buildArchiveInputStream() {
            return buildTarArchiveInputStream(inputStream);
        }

        /** {@inheritDoc} */
        @Override
        public TarArchiveExtractor build() throws IOException {
            return new TarArchiveExtractor(this);
        }
    }
}
