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

import static io.github.compress4j.utils.FileUtils.NO_MODE;
import static java.util.zip.ZipEntry.DEFLATED;
import static java.util.zip.ZipEntry.STORED;
import static org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.DEFAULT_COMPRESSION;
import static org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy.NEVER;

import io.github.compress4j.archivers.ArchiveCreator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.Zip64RequiredException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.commons.io.IOUtils;

/**
 * The Zip archive creator.
 *
 * @since 2.2
 */
public class ZipArchiveCreator extends ArchiveCreator<ZipArchiveOutputStream> {

    /**
     * Create a new ZipArchiveCreator with the given output stream.
     *
     * @param zipArchiveOutputStream the output Zip Archive Output Stream
     */
    public ZipArchiveCreator(ZipArchiveOutputStream zipArchiveOutputStream) {
        super(zipArchiveOutputStream);
    }

    /**
     * Create a new ZipArchiveCreator with the given output stream and options.
     *
     * @param builder the archive output stream builder
     * @throws IOException if an I/O error occurred
     */
    public ZipArchiveCreator(ZipArchiveCreatorBuilder builder) throws IOException {
        super(builder.buildArchiveOutputStream());
    }

    /**
     * Helper static method to create an instance of the {@link ZipArchiveCreatorBuilder}
     *
     * @param path the path to write the archive to
     * @return An instance of the {@link ZipArchiveCreatorBuilder}
     * @throws IOException if an I/O error occurred
     */
    public static ZipArchiveCreatorBuilder builder(Path path) throws IOException {
        return new ZipArchiveCreatorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link ZipArchiveCreatorBuilder}
     *
     * @param outputStream the output stream to write the archive to
     * @return An instance of the {@link ZipArchiveCreatorBuilder}
     */
    public static ZipArchiveCreatorBuilder builder(OutputStream outputStream) {
        return new ZipArchiveCreatorBuilder(outputStream);
    }

    private static ZipArchiveEntry getArchiveEntry(
            String name, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Path> symlinkTarget) {
        if (symlinkTarget.isPresent()) {
            // ZIP doesn't support symbolic links in the same way as TAR
            // We'll store symlinks as regular files containing the target path
            var entry = new ZipArchiveEntry(name);
            entry.setSize(symlinkTarget.get().toString().getBytes().length);
            return entry;
        } else {
            return new ZipArchiveEntry(name);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writeDirectoryEntry(String name, FileTime modTime) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(name + '/');
        entry.setTime(modTime);
        archiveOutputStream.putArchiveEntry(entry);
        archiveOutputStream.closeArchiveEntry();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeFileEntry(
            String name, InputStream inputStream, long size, FileTime modTime, int mode, Optional<Path> symlinkTarget)
            throws IOException {
        ZipArchiveEntry entry = getArchiveEntry(name, symlinkTarget);
        entry.setTime(modTime);
        entry.setSize(size);

        // Set Unix permissions if available
        if (mode != NO_MODE) {
            entry.setUnixMode(mode);
        }

        archiveOutputStream.putArchiveEntry(entry);

        if (symlinkTarget.isPresent()) {
            // Write symlink target as file content
            archiveOutputStream.write(symlinkTarget.get().toString().getBytes());
        } else {
            IOUtils.copy(inputStream, archiveOutputStream);
        }

        archiveOutputStream.closeArchiveEntry();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        archiveOutputStream.close();
    }

    /** Zip creator builder */
    public static class ZipArchiveCreatorBuilder
            extends ArchiveCreatorBuilder<ZipArchiveOutputStream, ZipArchiveCreatorBuilder, ZipArchiveCreator> {

        /** The file comment. */
        private String comment = "";

        /** Compression level for next entry. */
        private int level = DEFAULT_COMPRESSION;

        /**
         * The encoding to use for file names and the file comment.
         *
         * <p>For a list of possible values see <a
         * href="https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html">Supported Encodings</a>.
         * Defaults to UTF-8.
         */
        private String encoding = "UTF-8";

        /** Default compression method for next entry. */
        private int method = DEFLATED;

        private Zip64Mode zip64Mode = Zip64Mode.AsNeeded;

        /** whether to create UnicodePathExtraField-s for each entry. */
        private UnicodeExtraFieldPolicy createUnicodeExtraFields = NEVER;

        /** Whether to encode non-encodable file names as UTF-8. */
        private boolean fallbackToUtf8;

        /** whether to use the general purpose bit flag when writing UTF-8 file names or not. */
        private boolean useUtf8Flag = true;

        /**
         * Create a new {@link ZipArchiveCreator} with the given path.
         *
         * @param path the path to write the archive to
         * @throws IOException if an I/O error occurred
         */
        public ZipArchiveCreatorBuilder(Path path) throws IOException {
            this(Files.newOutputStream(path));
        }

        /**
         * Create a new {@link ZipArchiveCreator} with the given output stream.
         *
         * @param outputStream the output stream
         */
        public ZipArchiveCreatorBuilder(OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        protected ZipArchiveCreatorBuilder getThis() {
            return this;
        }

        /**
         * Set the compression level for the ZIP archive.
         *
         * @param compressionLevel the compression level (0-9, where 0 is no compression and 9 is maximum compression)
         * @return this builder
         */
        public ZipArchiveCreatorBuilder compressionLevel(int compressionLevel) {
            if (compressionLevel < 0 || compressionLevel > 9) {
                throw new IllegalArgumentException("Compression level must be between 0 and 9");
            }
            this.level = compressionLevel;
            return this;
        }

        /**
         * Set the compression method for the ZIP archive.
         *
         * @param compressionMethod the compression method (STORED or DEFLATED)
         * @return this builder
         */
        public ZipArchiveCreatorBuilder compressionMethod(int compressionMethod) {
            if (compressionMethod != STORED && compressionMethod != DEFLATED) {
                throw new IllegalArgumentException("Compression method must be STORED or DEFLATED");
            }
            this.method = compressionMethod;
            return this;
        }

        /**
         * Sets the file comment.
         *
         * @param comment the comment
         * @return this builder
         */
        public ZipArchiveCreatorBuilder setComment(final String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Sets whether Zip64 extensions will be used.
         *
         * <p>When setting the mode to {@link Zip64Mode#Never Never}, {@link ZipArchiveOutputStream#putArchiveEntry},
         * {@link ZipArchiveOutputStream#closeArchiveEntry}, {@link ZipArchiveOutputStream#finish} or
         * {@link ZipArchiveOutputStream#close} may throw a {@link Zip64RequiredException} if the entry's size or the
         * total size of the archive exceeds 4GB or there are more than 65,536 entries inside the archive. Any archive
         * created in this mode will be readable by implementations that don't support Zip64.
         *
         * <p>When setting the mode to {@link Zip64Mode#Always Always}, Zip64 extensions will be used for all entries.
         * Any archive created in this mode may be unreadable by implementations that don't support Zip64 even if all
         * its contents would be.
         *
         * <p>When setting the mode to {@link Zip64Mode#AsNeeded AsNeeded}, Zip64 extensions will transparently be used
         * for those entries that require them. This mode can only be used if the uncompressed size of the
         * {@link ZipArchiveEntry} is known when calling {@link ZipArchiveOutputStream#putArchiveEntry} or the archive
         * is written to a seekable output (i.e. you have used the
         * {@link ZipArchiveOutputStream#ZipArchiveOutputStream(java.io.File) File-arg constructor}) - this mode is not
         * valid when the output stream is not seekable and the uncompressed size is unknown when
         * {@link ZipArchiveOutputStream#putArchiveEntry} is called.
         *
         * <p>If no entry inside the resulting archive requires Zip64 extensions then {@link Zip64Mode#Never Never} will
         * create the smallest archive. {@link Zip64Mode#AsNeeded AsNeeded} will create a slightly bigger archive if the
         * uncompressed size of any entry has initially been unknown and create an archive identical to
         * {@link Zip64Mode#Never Never} otherwise. {@link Zip64Mode#Always Always} will create an archive that is at
         * least 24 bytes per entry bigger than the one {@link Zip64Mode#Never Never} would create.
         *
         * <p>Defaults to {@link Zip64Mode#AsNeeded AsNeeded} unless {@link ZipArchiveOutputStream#putArchiveEntry} is
         * called with an entry of unknown size and data is written to a non-seekable stream - in this case the default
         * is {@link Zip64Mode#Never Never}.
         *
         * @param mode Whether Zip64 extensions will be used.
         * @return this builder
         */
        public ZipArchiveCreatorBuilder setUseZip64(final Zip64Mode mode) {
            zip64Mode = mode;
            return this;
        }

        /**
         * Sets whether to create Unicode Extra Fields.
         *
         * <p>Defaults to NEVER.
         *
         * @param b whether to create Unicode Extra Fields.
         * @return this builder
         */
        public ZipArchiveCreatorBuilder setCreateUnicodeExtraFields(final UnicodeExtraFieldPolicy b) {
            this.createUnicodeExtraFields = b;
            return this;
        }

        /**
         * Sets whether to fall back to UTF and the language encoding flag if the file name cannot be encoded using the
         * specified encoding.
         *
         * <p>Defaults to false.
         *
         * @param fallbackToUTF8 whether to fall back to UTF and the language encoding flag if the file name cannot be
         *     encoded using the specified encoding.
         * @return this builder
         */
        public ZipArchiveCreatorBuilder setFallbackToUTF8(final boolean fallbackToUTF8) {
            this.fallbackToUtf8 = fallbackToUTF8;
            return this;
        }

        /**
         * Sets whether to set the language encoding flag if the file name encoding is UTF-8.
         *
         * <p>Defaults to true.
         *
         * @param b whether to set the language encoding flag if the file name encoding is UTF-8
         * @return this builder
         */
        public ZipArchiveCreatorBuilder setUseLanguageEncodingFlag(final boolean b) {
            this.useUtf8Flag = b;
            return this;
        }

        /**
         * The encoding to use for file names and the file comment.
         *
         * <p>For a list of possible values see <a
         * href="https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html">Supported Encodings</a>.
         * Defaults to UTF-8.
         *
         * @param encoding the encoding to use for file names, use null for the platform's default encoding
         * @return this builder
         */
        public ZipArchiveCreatorBuilder setEncoding(final String encoding) {
            this.encoding = encoding;
            return this;
        }

        /**
         * Build the ZipArchiveOutputStream.
         *
         * @return the configured ZipArchiveOutputStream
         */
        public ZipArchiveOutputStream buildArchiveOutputStream() {
            ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(outputStream);
            zipOut.setLevel(level);
            zipOut.setMethod(method);
            zipOut.setComment(comment);
            zipOut.setUseZip64(zip64Mode);
            zipOut.setCreateUnicodeExtraFields(createUnicodeExtraFields);
            zipOut.setFallbackToUTF8(fallbackToUtf8);
            zipOut.setUseLanguageEncodingFlag(useUtf8Flag);
            zipOut.setEncoding(encoding);
            return zipOut;
        }

        /**
         * Build the ZipArchiveCreator.
         *
         * @return the configured ZipArchiveCreator
         * @throws IOException if an I/O error occurred
         */
        public ZipArchiveCreator build() throws IOException {
            return new ZipArchiveCreator(this);
        }
    }
}
