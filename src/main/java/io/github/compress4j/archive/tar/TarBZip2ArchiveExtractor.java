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
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Tar BZip2 ArchiveExtractor
 *
 * @since 2.2
 */
public class TarBZip2ArchiveExtractor extends BaseTarArchiveExtractor {

    /**
     * Create a new {@link TarBZip2ArchiveExtractor} with the given input stream.
     *
     * @param tarArchiveInputStream the input Tar Archive Input Stream
     */
    public TarBZip2ArchiveExtractor(TarArchiveInputStream tarArchiveInputStream) {
        super(tarArchiveInputStream);
    }

    /**
     * Create a new {@link TarBZip2ArchiveExtractor} with the given input stream and options.
     *
     * @param builder the archive input stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarBZip2ArchiveExtractor(TarBZip2ArchiveExtractorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link TarBZip2ArchiveExtractorBuilder}
     *
     * @param path the path to write the archive to
     * @return An instance of the {@link TarBZip2ArchiveExtractorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static TarBZip2ArchiveExtractorBuilder builder(Path path) throws IOException {
        return new TarBZip2ArchiveExtractorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link TarBZip2ArchiveExtractorBuilder}
     *
     * @param inputStream the input stream
     * @return An instance of the {@link TarBZip2ArchiveExtractorBuilder}
     */
    public static TarBZip2ArchiveExtractorBuilder builder(InputStream inputStream) {
        return new TarBZip2ArchiveExtractorBuilder(inputStream);
    }

    public static class TarBZip2ArchiveExtractorBuilder
            extends BaseTarArchiveExtractorBuilder<TarBZip2ArchiveExtractorBuilder, TarBZip2ArchiveExtractor> {

        /**
         * Create a new {@link TarBZip2ArchiveExtractorBuilder} with the given path.
         *
         * @param path the path to write the archive to
         * @throws IOException if an I/O error occurred
         */
        public TarBZip2ArchiveExtractorBuilder(Path path) throws IOException {
            this(Files.newInputStream(path));
        }

        /**
         * Create a new {@link TarBZip2ArchiveExtractorBuilder} with the given input stream.
         *
         * @param inputStream the input stream
         */
        protected TarBZip2ArchiveExtractorBuilder(InputStream inputStream) {
            super(inputStream);
        }

        /** {@inheritDoc} */
        @Override
        protected TarBZip2ArchiveExtractorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public TarArchiveInputStream buildArchiveInputStream() throws IOException {
            return super.buildTarArchiveInputStream(new BZip2CompressorInputStream(inputStream));
        }

        /** {@inheritDoc} */
        @Override
        public TarBZip2ArchiveExtractor build() throws IOException {
            return new TarBZip2ArchiveExtractor(this);
        }
    }
}
