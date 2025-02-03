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
package io.github.compress4j.archive.compression;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * The Tar compressor.
 *
 * @since 2.2
 */
public class TarCompressor extends BaseTarCompressor {

    /**
     * Create a new TarCompressor with the given output stream.
     *
     * @param tarArchiveOutputStream the output Tar Archive Output Stream
     */
    public TarCompressor(TarArchiveOutputStream tarArchiveOutputStream) {
        super(tarArchiveOutputStream);
    }

    /**
     * Create a new TarCompressor with the given output stream and options.
     *
     * @param builder the archive output stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarCompressor(TarCompressorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link TarCompressorBuilder}
     *
     * @param path the path to write the archive to
     * @return An instance of the {@link TarCompressorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static TarCompressorBuilder builder(Path path) throws IOException {
        return new TarCompressorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link TarCompressorBuilder}
     *
     * @param outputStream the output stream
     * @return An instance of the {@link TarCompressorBuilder}
     */
    public static TarCompressorBuilder builder(OutputStream outputStream) {
        return new TarCompressorBuilder(outputStream);
    }

    /** Tar compressor builder */
    public static class TarCompressorBuilder extends BaseTarCompressorBuilder<TarCompressorBuilder, TarCompressor> {
        /**
         * Create a new {@link TarCompressor} with the given path.
         *
         * @param path the path to write the archive to
         * @throws IOException if an I/O error occurred
         */
        public TarCompressorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Create a new {@link TarCompressor} with the given output stream.
         *
         * @param outputStream the output stream
         */
        public TarCompressorBuilder(OutputStream outputStream) {
            super(outputStream);
        }

        /** {@inheritDoc} */
        @Override
        protected TarCompressorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public TarArchiveOutputStream buildArchiveOutputStream() {
            return buildTarArchiveOutputStream(outputStream);
        }

        /** {@inheritDoc} */
        @Override
        public TarCompressor build() throws IOException {
            return new TarCompressor(this);
        }
    }
}
