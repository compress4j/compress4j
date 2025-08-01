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
package io.github.compress4j.archivers.tar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * Tar Gz ArchiveExtractor
 *
 * @since 2.2
 */
public class TarGzArchiveExtractor extends BaseTarArchiveExtractor {

    /**
     * Create a new {@link TarGzArchiveExtractor} with the given input stream.
     *
     * @param tarArchiveInputStream the input Tar Archive Input Stream
     */
    public TarGzArchiveExtractor(TarArchiveInputStream tarArchiveInputStream) {
        super(tarArchiveInputStream);
    }

    /**
     * Create a new {@link TarGzArchiveExtractor} with the given input stream and options.
     *
     * @param builder the archive input stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarGzArchiveExtractor(TarGzArchiveExtractorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link TarGzArchiveExtractorBuilder}
     *
     * @param path the path to write the archive to
     * @return An instance of the {@link TarGzArchiveExtractorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static TarGzArchiveExtractorBuilder builder(Path path) throws IOException {
        return new TarGzArchiveExtractorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link TarGzArchiveExtractorBuilder}
     *
     * @param inputStream the input stream
     * @return An instance of the {@link TarGzArchiveExtractorBuilder}
     */
    public static TarGzArchiveExtractorBuilder builder(InputStream inputStream) {
        return new TarGzArchiveExtractorBuilder(inputStream);
    }

    /**
     * Builder for creating a {@link TarGzArchiveExtractor}.
     *
     * @since 2.2
     */
    public static class TarGzArchiveExtractorBuilder
            extends BaseTarArchiveExtractorBuilder<TarGzArchiveExtractorBuilder, TarGzArchiveExtractor> {

        /**
         * Create a new {@link TarGzArchiveExtractorBuilder} with the given path.
         *
         * @param path the path to write the archive to
         * @throws IOException if an I/O error occurred
         */
        public TarGzArchiveExtractorBuilder(Path path) throws IOException {
            this(Files.newInputStream(path));
        }

        /**
         * Create a new {@link TarGzArchiveExtractorBuilder} with the given input stream.
         *
         * @param inputStream the input stream
         */
        protected TarGzArchiveExtractorBuilder(InputStream inputStream) {
            super(inputStream);
        }

        /** {@inheritDoc} */
        @Override
        protected TarGzArchiveExtractorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public TarArchiveInputStream buildArchiveInputStream() throws IOException {
            return super.buildTarArchiveInputStream(new GzipCompressorInputStream(inputStream));
        }

        /** {@inheritDoc} */
        @Override
        public TarGzArchiveExtractor build() throws IOException {
            return new TarGzArchiveExtractor(this);
        }
    }
}
