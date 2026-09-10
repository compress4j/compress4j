/*
 * Copyright 2025-2026 The Compress4J Project
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
package io.github.compress4j.archivers.ar;

import io.github.compress4j.archivers.ArchiveCreator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * The AR archive creator.
 *
 * @since 2.2
 */
public class ArArchiveCreator extends ArchiveCreator<ArArchiveOutputStream> {

    /**
     * Unix {@code S_IFLNK} file-type bits (octal {@code 0120000}). Classic AR's mode field carries permission bits
     * only, with no type-bit concept, so compress4j marks a symlink entry by setting these bits in it and writes the
     * target path as the entry's content — similar in spirit to how {@code CpioArchiveCreator} encodes symlinks via
     * mode-field type bits, though CPIO's format defines those bits natively while AR's does not. Third-party AR
     * archives don't set this bit in practice, so reading them is unaffected.
     */
    @SuppressWarnings("OctalInteger")
    static final int S_IFLNK = 0120000;

    /** Mask isolating the Unix file-type bits within a mode value. */
    @SuppressWarnings("OctalInteger")
    static final int S_IFMT = 0170000;

    /**
     * Create a new ArArchiveCreator with the given output stream.
     *
     * @param arArchiveOutputStream the output AR Archive Output Stream
     */
    public ArArchiveCreator(ArArchiveOutputStream arArchiveOutputStream) {
        super(arArchiveOutputStream);
    }

    /**
     * Create a new ArArchiveCreator with the given output stream and options.
     *
     * @param builder the archive output stream builder
     * @throws IOException if an I/O error occurred
     */
    public ArArchiveCreator(ArArchiveCreatorBuilder builder) throws IOException {
        super(builder);
    }

    /**
     * Helper static method to create an instance of the {@link ArArchiveCreatorBuilder}
     *
     * @param path the path to write the archive to
     * @return An instance of the {@link ArArchiveCreatorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static ArArchiveCreatorBuilder builder(Path path) throws IOException {
        return new ArArchiveCreatorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link ArArchiveCreatorBuilder}
     *
     * @param outputStream the output stream to write the archive to
     * @return An instance of the {@link ArArchiveCreatorBuilder}
     */
    public static ArArchiveCreatorBuilder builder(OutputStream outputStream) {
        return new ArArchiveCreatorBuilder(outputStream);
    }

    /**
     * AR format doesn't support directories - skip them
     *
     * <p>{@inheritDoc}
     */
    @Override
    protected void writeDirectoryEntry(String name, FileTime modTime) {
        // AR format doesn't support directories - skip them
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("OctalInteger")
    protected void writeFileEntry(
            String name, InputStream source, long length, FileTime modTime, int mode, Optional<Path> symlinkTarget)
            throws IOException {

        byte[] content;
        int entryMode;
        if (symlinkTarget.isPresent()) {
            content = symlinkTarget.get().toString().getBytes(StandardCharsets.UTF_8);
            entryMode = S_IFLNK | (mode & 0777);
        } else {
            content = length < 0 ? IOUtils.toByteArray(source) : null;
            entryMode = mode;
        }
        long entryLength = content != null ? content.length : length;

        ArArchiveEntry entry = new ArArchiveEntry(name, entryLength, 0, 0, entryMode, modTime.toMillis() / 1000);
        archiveOutputStream.putArchiveEntry(entry);

        if (content != null) {
            archiveOutputStream.write(content);
        } else if (entryLength > 0) {
            IOUtils.copy(source, archiveOutputStream);
        }

        archiveOutputStream.closeArchiveEntry();
    }

    /** AR archive creator builder */
    public static class ArArchiveCreatorBuilder
            extends ArchiveCreatorBuilder<ArArchiveOutputStream, ArArchiveCreatorBuilder, ArArchiveCreator> {

        /**
         * Create a new {@link ArArchiveCreator} with the given path.
         *
         * @param path the path to write the archive to
         * @throws IOException if an I/O error occurred
         */
        public ArArchiveCreatorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Create a new {@link ArArchiveCreator} with the given output stream.
         *
         * @param outputStream the output stream
         */
        public ArArchiveCreatorBuilder(OutputStream outputStream) {
            super(outputStream);
        }

        /** {@inheritDoc} */
        @Override
        protected ArArchiveCreatorBuilder getThis() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ArArchiveOutputStream buildArchiveOutputStream() {
            return new ArArchiveOutputStream(outputStream);
        }

        /** {@inheritDoc} */
        @Override
        public ArArchiveCreator build() throws IOException {
            return new ArArchiveCreator(this);
        }
    }
}
