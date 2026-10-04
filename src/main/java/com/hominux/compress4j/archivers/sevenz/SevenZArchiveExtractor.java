/*
 * Copyright 2024-2026 The Compress4J Project
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
package com.hominux.compress4j.archivers.sevenz;

import com.hominux.compress4j.archivers.ArchiveExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

/**
 * 7z Archive Extractor.
 *
 * @since 3.2
 */
public class SevenZArchiveExtractor extends ArchiveExtractor<SevenZFileArchiveInputStream> {

    static final int UNIX_EXTENSION = 0x8000;
    static final int S_IFMT = 0170000;
    static final int S_IFLNK = 0120000;
    static final long MAX_SYMLINK_TARGET_BYTES = 4096;

    /**
     * Create a new {@link SevenZArchiveExtractor} with the given input stream.
     *
     * @param archiveInputStream the input 7z Archive Input Stream
     */
    public SevenZArchiveExtractor(SevenZFileArchiveInputStream archiveInputStream) {
        super(archiveInputStream);
    }

    /**
     * Create a new {@link SevenZArchiveExtractor} with the given builder.
     *
     * @param builder the archive input stream builder
     * @throws IOException if an I/O error occurred
     */
    public SevenZArchiveExtractor(SevenZArchiveExtractorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link SevenZArchiveExtractorBuilder}.
     *
     * @param path the path to the archive to extract
     * @return An instance of the {@link SevenZArchiveExtractorBuilder}
     */
    public static SevenZArchiveExtractorBuilder builder(Path path) {
        return new SevenZArchiveExtractorBuilder(path);
    }

    /** {@inheritDoc} */
    @Override
    protected Optional<Entry> nextEntry() throws IOException {
        SevenZArchiveEntry entry = archiveInputStream.getNextEntry();
        if (entry == null) {
            return Optional.empty();
        }
        String name = Objects.requireNonNull(entry.getName(), "7z entry has no name");
        int mode = unixMode(entry);
        Date modified = entry.getHasLastModifiedDate() ? entry.getLastModifiedDate() : null;
        Entry result;
        if (entry.isDirectory()) {
            result = new Entry(name, Entry.Type.DIR, mode);
        } else if ((mode & S_IFMT) == S_IFLNK) {
            result = new Entry(name, Entry.Type.SYMLINK, mode).withLinkTarget(readSymlinkTarget(entry));
        } else {
            result = new Entry(name, Entry.Type.FILE, mode);
        }
        return Optional.of(result.withMetadata(modified, result.type() == Entry.Type.FILE ? entry.getSize() : 0));
    }

    private String readSymlinkTarget(SevenZArchiveEntry entry) throws IOException {
        if (entry.getSize() > MAX_SYMLINK_TARGET_BYTES) {
            throw new IOException("Symlink target of '" + entry.getName() + "' exceeds " + MAX_SYMLINK_TARGET_BYTES
                    + " bytes: " + entry.getSize());
        }
        byte[] target = readEntryContent(entry.getName(), archiveInputStream, entry.getSize());
        return new String(target, StandardCharsets.UTF_8);
    }

    private static int unixMode(SevenZArchiveEntry entry) {
        if (!entry.getHasWindowsAttributes() || (entry.getWindowsAttributes() & UNIX_EXTENSION) == 0) {
            return 0;
        }
        return entry.getWindowsAttributes() >>> 16;
    }

    /** {@inheritDoc} */
    @Override
    protected InputStream openEntryStream(Entry entry) {
        return archiveInputStream;
    }

    /** 7z archive extractor builder. */
    public static class SevenZArchiveExtractorBuilder
            extends ArchiveExtractorBuilder<
                    SevenZFileArchiveInputStream, SevenZArchiveExtractorBuilder, SevenZArchiveExtractor> {

        private final Path origin;
        private SeekableByteChannel seekableByteChannel;
        private char[] password;

        /**
         * Create a new builder for the archive at the given path.
         *
         * @param path the path to the archive to extract
         */
        public SevenZArchiveExtractorBuilder(Path path) {
            this.origin = path;
        }

        /**
         * Sets the password used to decrypt encrypted entries.
         *
         * @param password the password; the array is copied
         * @return {@code this} instance.
         */
        public SevenZArchiveExtractorBuilder password(char[] password) {
            this.password = password == null ? null : password.clone();
            return this;
        }

        /**
         * The actual channel, overrides the path.
         *
         * @param seekableByteChannel The actual channel.
         * @return {@code this} instance.
         */
        public SevenZArchiveExtractorBuilder setSeekableByteChannel(SeekableByteChannel seekableByteChannel) {
            this.seekableByteChannel = seekableByteChannel;
            return this;
        }

        @Override
        public SevenZArchiveExtractorBuilder getThis() {
            return this;
        }

        /**
         * Build the {@link SevenZFileArchiveInputStream}.
         *
         * @return the configured input stream
         * @throws IOException if the archive cannot be opened, for example because the password is wrong
         */
        @Override
        public SevenZFileArchiveInputStream buildArchiveInputStream() throws IOException {
            SevenZFile.Builder file = SevenZFile.builder().setUseDefaultNameForUnnamedEntries(true);
            if (seekableByteChannel != null) {
                file.setSeekableByteChannel(seekableByteChannel);
            } else {
                file.setPath(origin);
            }
            if (password != null) {
                file.setPassword(password);
            }
            return new SevenZFileArchiveInputStream(file.get());
        }

        /**
         * Build the {@link SevenZArchiveExtractor}.
         *
         * @return the configured extractor
         * @throws IOException if an I/O error occurred
         */
        @Override
        public SevenZArchiveExtractor build() throws IOException {
            return new SevenZArchiveExtractor(this);
        }
    }
}
