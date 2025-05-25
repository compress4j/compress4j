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
package io.github.compress4j.archive.tar;

import io.github.compress4j.compressors.bzip2.BZip2Compressor.BZip2CompressorOutputStreamBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * The Tar bzip2 creator.
 *
 * @since 2.2
 */
public class TarBZip2ArchiveCreator extends BaseTarArchiveCreator {

    /**
     * Create a new {@link TarBZip2ArchiveCreator} with the given output stream.
     *
     * @param tarArchiveOutputStream the output Tar Archive Output Stream
     */
    public TarBZip2ArchiveCreator(TarArchiveOutputStream tarArchiveOutputStream) {
        super(tarArchiveOutputStream);
    }

    /**
     * Create a new {@link TarBZip2ArchiveCreator} with the given output stream and options.
     *
     * @param builder the archive output stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarBZip2ArchiveCreator(TarBZip2ArchiveCreatorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link TarBZip2ArchiveCreatorBuilder}
     *
     * @param path the path to write the archive to
     * @return An instance of the {@link TarBZip2ArchiveCreatorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static TarBZip2ArchiveCreatorBuilder builder(Path path) throws IOException {
        return new TarBZip2ArchiveCreatorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link TarBZip2ArchiveCreatorBuilder}
     *
     * @param outputStream the output stream
     * @return An instance of the {@link TarBZip2ArchiveCreatorBuilder}
     */
    public static TarBZip2ArchiveCreatorBuilder builder(OutputStream outputStream) {
        return new TarBZip2ArchiveCreatorBuilder(outputStream);
    }

    public static class TarBZip2ArchiveCreatorBuilder
            extends BaseTarArchiveCreatorBuilder<TarBZip2ArchiveCreatorBuilder, TarBZip2ArchiveCreator> {

        private final BZip2CompressorOutputStreamBuilder<TarBZip2ArchiveCreatorBuilder> compressorOutputStreamBuilder;

        /**
         * Create a new {@link TarBZip2ArchiveCreatorBuilder} with the given path.
         *
         * @param path the path to write the archive to
         * @throws IOException if an I/O error occurred
         */
        public TarBZip2ArchiveCreatorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Create a new {@link TarBZip2ArchiveCreatorBuilder} with the given output stream.
         *
         * @param outputStream the output stream
         */
        protected TarBZip2ArchiveCreatorBuilder(OutputStream outputStream) {
            super(outputStream);
            this.compressorOutputStreamBuilder = new BZip2CompressorOutputStreamBuilder<>(this, this.outputStream);
        }

        /** {@inheritDoc} */
        @Override
        protected TarBZip2ArchiveCreatorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public TarArchiveOutputStream buildArchiveOutputStream() throws IOException {
            return super.buildTarArchiveOutputStream(compressorOutputStreamBuilder.build());
        }

        public BZip2CompressorOutputStreamBuilder<TarBZip2ArchiveCreatorBuilder> compressorOutputStreamBuilder() {
            return compressorOutputStreamBuilder;
        }

        /** {@inheritDoc} */
        @Override
        public TarBZip2ArchiveCreator build() throws IOException {
            return new TarBZip2ArchiveCreator(this);
        }
    }
}
