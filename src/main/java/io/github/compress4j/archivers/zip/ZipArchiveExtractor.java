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
package io.github.compress4j.archivers.zip;

import io.github.compress4j.archivers.ArchiveExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.io.function.IOFunction;

/**
 * Zip Archive Extractor
 *
 * @since 2.2
 */
public class ZipArchiveExtractor extends ArchiveExtractor<ZipFileArchiveInputStream> {

    /**
     * Create a new {@link ZipArchiveExtractor} with the given input stream.
     *
     * @param zipFileArchiveInputStream the input Zip Archive Input Stream
     */
    public ZipArchiveExtractor(ZipFileArchiveInputStream zipFileArchiveInputStream) {
        super(zipFileArchiveInputStream);
    }

    /**
     * Create a new ZipArchiveExtractor with the given input stream and options.
     *
     * @param builder the archive input stream builder
     * @throws IOException if an I/O error occurred
     */
    public ZipArchiveExtractor(ZipArchiveExtractorBuilder builder) throws IOException {
        super(builder.buildArchiveInputStream());
    }

    /**
     * Helper static method to create an instance of the {@link ZipArchiveExtractorBuilder}
     *
     * @param path the path to the archive to extract
     * @return An instance of the {@link ZipArchiveExtractorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static ZipArchiveExtractorBuilder builder(Path path) throws IOException {
        return new ZipArchiveExtractorBuilder(path);
    }

    /** {@inheritDoc} */
    @Override
    public Entry nextEntry() throws IOException {
        ZipArchiveEntry ze = archiveInputStream.getNextEntry();
        if (ze == null) {
            return null;
        } else {
            return new Entry(
                    ze.getName(), type(ze), ze.getUnixMode(), archiveInputStream.getUnixSymlink(ze), ze.getSize());
        }
    }

    private static Entry.Type type(ZipArchiveEntry ze) {
        if (ze.isUnixSymlink()) {
            return Entry.Type.SYMLINK;
        } else if (ze.isDirectory()) {
            return Entry.Type.DIR;
        } else {
            return Entry.Type.FILE;
        }
    }

    /** {@inheritDoc} */
    @Override
    public InputStream openEntryStream(Entry entry) {
        return archiveInputStream;
    }

    /**
     * Builder for creating a {@link ZipArchiveExtractor}.
     *
     * @since 2.2
     */
    public static class ZipArchiveExtractorBuilder
            extends ArchiveExtractorBuilder<
                    ZipFileArchiveInputStream, ZipArchiveExtractorBuilder, ZipArchiveExtractor> {

        private SeekableByteChannel seekableByteChannel;
        private boolean useUnicodeExtraFields = true;
        private boolean ignoreLocalFileHeader;
        private long maxNumberOfDisks = 1;
        private IOFunction<InputStream, InputStream> zstdInputStreamFactory;

        private final Path origin;

        /**
         * Create a new {@link ZipArchiveExtractor} with the given path.
         *
         * @param path the path to the archive to extract
         * @throws IOException if an I/O error occurred
         */
        public ZipArchiveExtractorBuilder(Path path) throws IOException {
            this.origin = path;
        }

        /**
         * Sets whether to ignore information stored inside the local file header.
         *
         * @param ignoreLocalFileHeader whether to ignore information stored inside.
         * @return {@code this} instance.
         */
        public ZipArchiveExtractorBuilder setIgnoreLocalFileHeader(final boolean ignoreLocalFileHeader) {
            this.ignoreLocalFileHeader = ignoreLocalFileHeader;
            return this;
        }

        /**
         * Sets max number of multi archive disks, default is 1 (no multi archive).
         *
         * @param maxNumberOfDisks max number of multi archive disks.
         * @return {@code this} instance.
         */
        public ZipArchiveExtractorBuilder setMaxNumberOfDisks(final long maxNumberOfDisks) {
            this.maxNumberOfDisks = maxNumberOfDisks;
            return this;
        }

        /**
         * The actual channel, overrides any other input aspects like a File, Path, and so on.
         *
         * @param seekableByteChannel The actual channel.
         * @return {@code this} instance.
         */
        public ZipArchiveExtractorBuilder setSeekableByteChannel(final SeekableByteChannel seekableByteChannel) {
            this.seekableByteChannel = seekableByteChannel;
            return this;
        }

        /**
         * Sets whether to use InfoZIP Unicode Extra Fields (if present) to set the file names.
         *
         * @param useUnicodeExtraFields whether to use InfoZIP Unicode Extra Fields (if present) to set the file names.
         * @return {@code this} instance.
         */
        public ZipArchiveExtractorBuilder setUseUnicodeExtraFields(final boolean useUnicodeExtraFields) {
            this.useUnicodeExtraFields = useUnicodeExtraFields;
            return this;
        }

        /**
         * Sets the factory {@link IOFunction} to create a Zstd {@link InputStream}. Defaults to
         * {@link ZstdCompressorInputStream#ZstdCompressorInputStream(InputStream)}.
         *
         * <p>Call this method to plugin an alternate Zstd input stream implementation.
         *
         * @param zstdInpStreamFactory the factory {@link IOFunction} to create a Zstd {@link InputStream}; {@code null}
         *     resets to the default.
         * @return {@code this} instance.
         */
        public ZipArchiveExtractorBuilder setZstdInputStreamFactory(
                final IOFunction<InputStream, InputStream> zstdInpStreamFactory) {
            this.zstdInputStreamFactory = zstdInpStreamFactory;
            return this;
        }

        @Override
        public ZipArchiveExtractorBuilder getThis() {
            return this;
        }

        /**
         * Build the ZipArchiveInputStream.
         *
         * @return the configured ZipArchiveInputStream
         */
        public ZipFileArchiveInputStream buildArchiveInputStream() throws IOException {
            var zipfile = ZipFile.builder()
                    .setIgnoreLocalFileHeader(ignoreLocalFileHeader)
                    .setMaxNumberOfDisks(maxNumberOfDisks)
                    .setSeekableByteChannel(seekableByteChannel)
                    .setUseUnicodeExtraFields(useUnicodeExtraFields)
                    .setZstdInputStreamFactory(zstdInputStreamFactory)
                    .setPath(origin)
                    .get();
            return new ZipFileArchiveInputStream(zipfile);
        }

        /**
         * Build the ZipArchiveExtractor.
         *
         * @return the configured ZipArchiveExtractor
         */
        public ZipArchiveExtractor build() throws IOException {
            return new ZipArchiveExtractor(this);
        }
    }
}
