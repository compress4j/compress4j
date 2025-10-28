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
package io.github.compress4j.archivers.tar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

/**
 * Tar XZ ArchiveExtractor
 *
 * @since 2.2
 */
public class TarXzArchiveExtractor extends BaseTarArchiveExtractor {

    /**
     * Create a new {@link TarXzArchiveExtractor} with the given input stream.
     *
     * @param tarArchiveInputStream the input Tar Archive Input Stream
     */
    public TarXzArchiveExtractor(TarArchiveInputStream tarArchiveInputStream) {
        super(tarArchiveInputStream);
    }

    /**
     * Create a new {@link TarXzArchiveExtractor} with the given input stream and options.
     *
     * @param builder the archive input stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarXzArchiveExtractor(TarXzArchiveExtractorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link TarXzArchiveExtractorBuilder}
     *
     * @param path the path to the archive to extract
     * @return An instance of the {@link TarXzArchiveExtractorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static TarXzArchiveExtractorBuilder builder(Path path) throws IOException {
        return new TarXzArchiveExtractorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link TarXzArchiveExtractorBuilder}
     *
     * @param inputStream the input stream of the archive to extract
     * @return An instance of the {@link TarXzArchiveExtractorBuilder}
     */
    public static TarXzArchiveExtractorBuilder builder(InputStream inputStream) {
        return new TarXzArchiveExtractorBuilder(inputStream);
    }

    /** Builder for creating instances of {@link TarXzArchiveExtractor}. */
    public static class TarXzArchiveExtractorBuilder
            extends BaseTarArchiveExtractorBuilder<TarXzArchiveExtractorBuilder, TarXzArchiveExtractor> {

        /**
         * Create a new {@link TarXzArchiveExtractorBuilder} with the given path.
         *
         * @param path the path to the archive to extract
         * @throws IOException if an I/O error occurred
         */
        public TarXzArchiveExtractorBuilder(Path path) throws IOException {
            this(Files.newInputStream(path));
        }

        /**
         * Create a new {@link TarXzArchiveExtractorBuilder} with the given input stream.
         *
         * @param inputStream the input stream
         */
        public TarXzArchiveExtractorBuilder(InputStream inputStream) {
            super(inputStream);
        }

        /** {@inheritDoc} */
        @Override
        public TarXzArchiveExtractorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public TarArchiveInputStream buildArchiveInputStream() throws IOException {
            return super.buildTarArchiveInputStream(new XZCompressorInputStream(inputStream));
        }

        /** {@inheritDoc} */
        @Override
        public TarXzArchiveExtractor build() throws IOException {
            return new TarXzArchiveExtractor(this);
        }
    }
}
