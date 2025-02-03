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
import java.util.zip.Deflater;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;

/**
 * The Tar Gz compressor.
 *
 * @since 2.2
 */
public class TarGzCompressor extends BaseTarCompressor {

    /**
     * Create a new {@link TarGzCompressor} with the given output stream.
     *
     * @param tarArchiveOutputStream the output Tar Archive Output Stream
     */
    public TarGzCompressor(TarArchiveOutputStream tarArchiveOutputStream) {
        super(tarArchiveOutputStream);
    }

    /**
     * Create a new {@link TarGzCompressor} with the given output stream and options.
     *
     * @param builder the archive output stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarGzCompressor(TarGzCompressorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link TarGzCompressorBuilder}
     *
     * @param path the path to write the archive to
     * @return An instance of the {@link TarGzCompressorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static TarGzCompressorBuilder builder(Path path) throws IOException {
        return new TarGzCompressorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link TarGzCompressorBuilder}
     *
     * @param outputStream the output stream
     * @return An instance of the {@link TarGzCompressorBuilder}
     */
    public static TarGzCompressorBuilder builder(OutputStream outputStream) {
        return new TarGzCompressorBuilder(outputStream);
    }

    public static class TarGzCompressorBuilder
            extends BaseTarCompressorBuilder<TarGzCompressorBuilder, TarGzCompressor> {

        private int bufferSize = 512;
        private String comment;
        private int compressionLevel = Deflater.DEFAULT_COMPRESSION;
        private int deflateStrategy = Deflater.DEFAULT_STRATEGY;
        private String fileName;
        private long modificationTime;
        private int operatingSystem = 255; // Unknown OS by default

        /**
         * Create a new {@link TarGzCompressorBuilder} with the given path.
         *
         * @param path the path to write the archive to
         * @throws IOException if an I/O error occurred
         */
        public TarGzCompressorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Create a new {@link TarGzCompressorBuilder} with the given output stream.
         *
         * @param outputStream the output stream
         */
        protected TarGzCompressorBuilder(OutputStream outputStream) {
            super(outputStream);
        }

        /**
         * Sets size of the buffer used to retrieve compressed data from {@link Deflater} and write to underlying
         * {@link OutputStream}.
         *
         * @param bufferSize the bufferSize to set. Must be a positive value.
         * @return the instance of the {@link TarGzCompressorBuilder}
         */
        public TarGzCompressorBuilder withBufferSize(final int bufferSize) {
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("invalid buffer size: " + bufferSize);
            }
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Adds comment to be added to the tar.gz file
         *
         * @param comment the comment to be added
         * @return the instance of the {@link TarGzCompressorBuilder}
         */
        public TarGzCompressorBuilder withComment(final String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Sets the compression level.
         *
         * @param compressionLevel the compression level (between 0 and 9)
         * @see Deflater#NO_COMPRESSION
         * @see Deflater#BEST_SPEED
         * @see Deflater#DEFAULT_COMPRESSION
         * @see Deflater#BEST_COMPRESSION
         * @return the instance of the {@link TarGzCompressorBuilder}
         */
        public TarGzCompressorBuilder withCompressionLevel(final int compressionLevel) {
            if (compressionLevel < -1 || compressionLevel > 9) {
                throw new IllegalArgumentException("Invalid gzip compression level: " + compressionLevel);
            }
            this.compressionLevel = compressionLevel;
            return this;
        }

        /**
         * Sets the deflater strategy.
         *
         * @param deflateStrategy the new compression strategy
         * @see Deflater#setStrategy(int)
         * @return the instance of the {@link TarGzCompressorBuilder}
         */
        public TarGzCompressorBuilder withDeflateStrategy(final int deflateStrategy) {
            this.deflateStrategy = deflateStrategy;
            return this;
        }

        /**
         * Sets the name of the compressed file.
         *
         * @param fileName the name of the file without the directory path
         * @return the instance of the {@link TarGzCompressorBuilder}
         */
        public TarGzCompressorBuilder withFileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Sets the modification time of the compressed file.
         *
         * @param modificationTime the modification time, in milliseconds
         * @return the instance of the {@link TarGzCompressorBuilder}
         */
        public TarGzCompressorBuilder withModificationTime(final long modificationTime) {
            this.modificationTime = modificationTime;
            return this;
        }

        /**
         * Sets the operating system on which the compression took place. The defined values are:
         *
         * <ul>
         *   <li>0: FAT file system (MS-DOS, OS/2, NT/Win32)
         *   <li>1: Amiga
         *   <li>2: VMS (or OpenVMS)
         *   <li>3: Unix
         *   <li>4: VM/CMS
         *   <li>5: Atari TOS
         *   <li>6: HPFS file system (OS/2, NT)
         *   <li>7: Macintosh
         *   <li>8: Z-System
         *   <li>9: CP/M
         *   <li>10: TOPS-20
         *   <li>11: NTFS file system (NT)
         *   <li>12: QDOS
         *   <li>13: Acorn RISCOS
         *   <li>255: Unknown
         * </ul>
         *
         * @param operatingSystem the code of the operating system
         * @return the instance of the {@link TarGzCompressorBuilder}
         */
        public TarGzCompressorBuilder withOperatingSystem(final int operatingSystem) {
            this.operatingSystem = operatingSystem;
            return this;
        }

        /** {@inheritDoc} */
        @Override
        protected TarGzCompressorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public TarArchiveOutputStream buildArchiveOutputStream() throws IOException {
            GzipParameters parameters = new GzipParameters();
            parameters.setBufferSize(bufferSize);
            parameters.setComment(comment);
            parameters.setCompressionLevel(compressionLevel);
            parameters.setDeflateStrategy(deflateStrategy);
            parameters.setFileName(fileName);
            parameters.setModificationTime(modificationTime);
            parameters.setOperatingSystem(operatingSystem);
            return super.buildTarArchiveOutputStream(new GzipCompressorOutputStream(outputStream, parameters));
        }

        /** {@inheritDoc} */
        @Override
        public TarGzCompressor build() throws IOException {
            return new TarGzCompressor(this);
        }
    }
}
