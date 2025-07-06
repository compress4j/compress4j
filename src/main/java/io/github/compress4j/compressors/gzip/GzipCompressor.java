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
package io.github.compress4j.compressors.gzip;

import io.github.compress4j.compressors.Compressor;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;

/**
 * This class provides a Gzip compressor that writes to a GzipCompressorOutputStream. It extends the Compressor class
 * and provides a builder for creating instances.
 */
public class GzipCompressor extends Compressor<GzipCompressorOutputStream> {

    /**
     * Constructor that takes a GzipCompressorOutputStream.
     *
     * @param compressorOutputStream the GzipCompressorOutputStream to write to.
     */
    public GzipCompressor(GzipCompressorOutputStream compressorOutputStream) {
        super(compressorOutputStream);
    }

    /**
     * Constructor that takes a GzipCompressorBuilder.
     *
     * @param builder the GzipCompressorBuilder to build from.
     * @throws IOException if an I/O error occurred
     */
    public GzipCompressor(GzipCompressorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link GzipCompressorBuilder}
     *
     * @param path the path to write the compressor to
     * @return An instance of the {@link GzipCompressorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static GzipCompressorBuilder builder(Path path) throws IOException {
        return new GzipCompressorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link GzipCompressorBuilder}
     *
     * @param outputStream the output stream
     * @return An instance of the {@link GzipCompressorBuilder}
     */
    public static GzipCompressorBuilder builder(OutputStream outputStream) {
        return new GzipCompressorBuilder(outputStream);
    }

    public static class GzipCompressorOutputStreamBuilder<P> {
        private final P parent;
        protected final OutputStream outputStream;
        private int bufferSize = 512;
        private String comment;
        private int compressionLevel = Deflater.DEFAULT_COMPRESSION;
        private int deflateStrategy = Deflater.DEFAULT_STRATEGY;
        private String fileName;
        private long modificationTime;
        private int operatingSystem = 255; // Unknown OS by default

        /**
         * Create a new {@link GzipCompressorBuilder} with the given output stream.
         *
         * @param parent parent builder calling this {@link GzipCompressorOutputStreamBuilder}
         * @param outputStream the output stream
         */
        public GzipCompressorOutputStreamBuilder(P parent, OutputStream outputStream) {
            this.parent = parent;
            this.outputStream = outputStream;
        }

        /**
         * Sets size of the buffer used to retrieve compressed data from {@link Deflater} and write to underlying
         * {@link OutputStream}.
         *
         * @param bufferSize the bufferSize to set. Must be a positive value.
         * @return the instance of the {@link GzipCompressorOutputStreamBuilder}
         */
        public GzipCompressorOutputStreamBuilder<P> bufferSize(final int bufferSize) {
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
         * @return the instance of the {@link GzipCompressorOutputStreamBuilder}
         */
        public GzipCompressorOutputStreamBuilder<P> comment(final String comment) {
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
         * @return the instance of the {@link GzipCompressorOutputStreamBuilder}
         */
        public GzipCompressorOutputStreamBuilder<P> compressionLevel(final int compressionLevel) {
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
         * @return the instance of the {@link GzipCompressorOutputStreamBuilder}
         */
        public GzipCompressorOutputStreamBuilder<P> deflateStrategy(final int deflateStrategy) {
            this.deflateStrategy = deflateStrategy;
            return this;
        }

        /**
         * Sets the name of the compressed file.
         *
         * @param fileName the name of the file without the directory path
         * @return the instance of the {@link GzipCompressorOutputStreamBuilder}
         */
        public GzipCompressorOutputStreamBuilder<P> fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Sets the modification time of the compressed file.
         *
         * @param modificationTime the modification time, in milliseconds
         * @return the instance of the {@link GzipCompressorOutputStreamBuilder}
         */
        public GzipCompressorOutputStreamBuilder<P> modificationTime(final long modificationTime) {
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
         * @return the instance of the {@link GzipCompressorOutputStreamBuilder}
         */
        public GzipCompressorOutputStreamBuilder<P> operatingSystem(final int operatingSystem) {
            this.operatingSystem = operatingSystem;
            return this;
        }

        public GzipCompressorOutputStream build() throws IOException {
            GzipParameters parameters = new GzipParameters();
            parameters.setBufferSize(bufferSize);
            parameters.setComment(comment);
            parameters.setCompressionLevel(compressionLevel);
            parameters.setDeflateStrategy(deflateStrategy);
            parameters.setFileName(fileName);
            parameters.setModificationTime(modificationTime);
            parameters.setOperatingSystem(operatingSystem);
            return new GzipCompressorOutputStream(outputStream, parameters);
        }

        public P parentBuilder() {
            return parent;
        }
    }

    public static class GzipCompressorBuilder
            extends CompressorBuilder<GzipCompressorOutputStream, GzipCompressorBuilder, GzipCompressor> {

        private final GzipCompressorOutputStreamBuilder<GzipCompressorBuilder> compressorOutputStreamBuilder;

        /**
         * Create a new {@link GzipCompressorBuilder} with the given path.
         *
         * @param path the path to write the compressor to
         * @throws IOException if an I/O error occurred
         */
        public GzipCompressorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Create a new {@link GzipCompressorBuilder} with the given output stream.
         *
         * @param outputStream the output stream
         */
        protected GzipCompressorBuilder(OutputStream outputStream) {
            super(outputStream);
            this.compressorOutputStreamBuilder = new GzipCompressorOutputStreamBuilder<>(this, this.outputStream);
        }

        public GzipCompressorOutputStreamBuilder<GzipCompressorBuilder> compressorOutputStreamBuilder() {
            return compressorOutputStreamBuilder;
        }

        @Override
        protected GzipCompressorBuilder getThis() {
            return this;
        }

        @Override
        public GzipCompressorOutputStream buildCompressorOutputStream() throws IOException {
            return compressorOutputStreamBuilder.build();
        }

        @Override
        public GzipCompressor build() throws IOException {
            return new GzipCompressor(this);
        }
    }
}
