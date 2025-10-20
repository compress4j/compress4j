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
package io.github.compress4j.archivers.cpio;

import io.github.compress4j.archivers.ArchiveCreator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioConstants;
import org.apache.commons.io.IOUtils;

/**
 * The CPIO archive creator.
 *
 * @since 2.2
 */
@SuppressWarnings("OctalInteger")
public class CpioArchiveCreator extends ArchiveCreator<CpioArchiveOutputStream> {

    private final short format;

    /**
     * Create a new CpioArchiveCreator with the given output stream.
     *
     * @param cpioArchiveOutputStream the output CPIO Archive Output Stream
     */
    public CpioArchiveCreator(CpioArchiveOutputStream cpioArchiveOutputStream) {
        super(cpioArchiveOutputStream);
        this.format = CpioConstants.FORMAT_NEW;
    }

    /**
     * Create a new CpioArchiveCreator with the given output stream and options.
     *
     * @param builder the archive output stream builder
     * @throws IOException if an I/O error occurred
     */
    public CpioArchiveCreator(CpioArchiveCreatorBuilder builder) throws IOException {
        super(builder);
        this.format = builder.cpioOutputStreamBuilder.format;
    }

    @Override
    protected void writeDirectoryEntry(String name, FileTime modTime) throws IOException {
        // Ensure directory names end with "/" as expected by CPIO format
        String directoryName = name.endsWith("/") ? name : name + "/";

        // Create entry with the format that matches the output stream
        CpioArchiveEntry entry;
        if (format != CpioConstants.FORMAT_NEW) {
            // Use explicit format for non-default formats
            entry = new CpioArchiveEntry(format, directoryName, 0);
        } else {
            // Use default constructor for FORMAT_NEW to maintain compatibility
            entry = new CpioArchiveEntry(directoryName);
            entry.setSize(0);
        }

        entry.setTime(modTime.toMillis() / 1000L);
        entry.setMode(CpioConstants.C_ISDIR | 0755);
        archiveOutputStream.putArchiveEntry(entry);
        archiveOutputStream.closeArchiveEntry();
    }

    @Override
    protected void writeFileEntry(
            String name, InputStream source, long length, FileTime modTime, int mode, Optional<Path> symlinkTarget)
            throws IOException {

        if (length < 0) {
            length = source.available();
        }

        // Create entry with the format that matches the output stream
        CpioArchiveEntry entry;
        if (format != CpioConstants.FORMAT_NEW) {
            // Use explicit format for non-default formats
            entry = new CpioArchiveEntry(format, name, length);
        } else {
            // Use default constructor for FORMAT_NEW to maintain compatibility
            entry = new CpioArchiveEntry(name);
            entry.setSize(length);
        }

        entry.setTime(modTime.toMillis() / 1000L); // CPIO uses seconds since epoch

        // Handle symbolic links
        if (symlinkTarget.isPresent()) {
            entry.setMode(CpioConstants.C_ISLNK | 0644); // Symbolic link with read/write permissions
            // For symbolic links, the content is the target path
            String linkTarget = symlinkTarget.get().toString();
            entry.setSize(linkTarget.length());
        } else {
            // Set appropriate file mode - use default file permissions if mode is 0 or invalid
            if (mode == 0) {
                entry.setMode(CpioConstants.C_ISREG | 0644); // Regular file with read/write permissions
            } else {
                // Try to use the provided mode, but ensure it's a valid CPIO mode
                try {
                    entry.setMode(CpioConstants.C_ISREG | (mode & 0777)); // Mask to only permission bits
                } catch (IllegalArgumentException e) {
                    // Fallback to default permissions if mode is invalid
                    entry.setMode(CpioConstants.C_ISREG | 0644);
                }
            }
        }

        archiveOutputStream.putArchiveEntry(entry);

        if (symlinkTarget.isPresent()) {
            // Write the symbolic link target as content
            archiveOutputStream.write(symlinkTarget.get().toString().getBytes(StandardCharsets.UTF_8));
        } else if (length > 0) {
            // Copy file content
            IOUtils.copy(source, archiveOutputStream);
        }

        archiveOutputStream.closeArchiveEntry();
    }

    /**
     * Helper static method to create an instance of the {@link CpioArchiveCreatorBuilder}
     *
     * @param path the path to write the archive to
     * @return a new instance of {@link CpioArchiveCreatorBuilder}
     * @throws IOException if an I/O error occurs opening the file
     */
    public static CpioArchiveCreatorBuilder builder(Path path) throws IOException {
        return new CpioArchiveCreatorBuilder(path);
    }

    /**
     * Helper static method to create an instance of the {@link CpioArchiveCreatorBuilder}
     *
     * @param outputStream the output stream to write the archive to
     * @return a new instance of {@link CpioArchiveCreatorBuilder}
     */
    public static CpioArchiveCreatorBuilder builder(OutputStream outputStream) {
        return new CpioArchiveCreatorBuilder(outputStream);
    }

    /**
     * Builder for configuring and creating a {@link CpioArchiveOutputStream}.
     *
     * @param <P> the type of the parent builder
     * @since 2.2
     */
    public static class CpioArchiveOutputStreamBuilder<P> {
        /** The output stream to write the archive to. */
        protected final OutputStream outputStream;

        private final P parent;
        private short format = CpioConstants.FORMAT_NEW;
        private int blockSize = CpioConstants.BLOCK_SIZE;
        private String encoding = "UTF-8";

        /**
         * Constructs a builder for a CPIO output stream.
         *
         * @param parent the parent builder
         * @param outputStream the output stream to write the archive to
         */
        public CpioArchiveOutputStreamBuilder(P parent, OutputStream outputStream) {
            this.parent = parent;
            this.outputStream = outputStream;
        }

        /**
         * Sets the CPIO format to use.
         *
         * @param format the CPIO format (e.g., CpioConstants.FORMAT_NEW, FORMAT_OLD_ASCII, etc.)
         * @return this builder instance
         */
        public CpioArchiveOutputStreamBuilder<P> format(short format) {
            this.format = format;
            return this;
        }

        /**
         * Sets the block size for the CPIO archive.
         *
         * @param blockSize the block size in bytes
         * @return this builder instance
         */
        public CpioArchiveOutputStreamBuilder<P> blockSize(int blockSize) {
            this.blockSize = blockSize;
            return this;
        }

        /**
         * Sets the character encoding for file names.
         *
         * @param encoding the character encoding
         * @return this builder instance
         */
        public CpioArchiveOutputStreamBuilder<P> encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        /**
         * Returns the parent builder.
         *
         * @return the parent builder
         */
        public P and() {
            return parent;
        }

        /**
         * Builds the {@link CpioArchiveOutputStream} with the configured options.
         *
         * @return a new CPIO archive output stream
         * @throws IOException if an I/O error occurs during stream creation
         */
        public CpioArchiveOutputStream build() throws IOException {
            return new CpioArchiveOutputStream(outputStream, format, blockSize, encoding);
        }
    }

    /**
     * Builder for configuring and creating {@link CpioArchiveCreator} instances.
     *
     * @since 2.2
     */
    public static class CpioArchiveCreatorBuilder
            extends ArchiveCreatorBuilder<CpioArchiveOutputStream, CpioArchiveCreatorBuilder, CpioArchiveCreator> {

        private final CpioArchiveOutputStreamBuilder<CpioArchiveCreatorBuilder> cpioOutputStreamBuilder;

        /**
         * Constructs a CpioArchiveCreatorBuilder with the given file path.
         *
         * @param path the file path to write the archive to
         * @throws IOException if an I/O error occurs opening the file
         */
        public CpioArchiveCreatorBuilder(Path path) throws IOException {
            super(Files.newOutputStream(path));
            this.cpioOutputStreamBuilder = new CpioArchiveOutputStreamBuilder<>(this, outputStream);
        }

        /**
         * Constructs a CpioArchiveCreatorBuilder with the given output stream.
         *
         * @param outputStream the output stream to write the archive to
         */
        public CpioArchiveCreatorBuilder(OutputStream outputStream) {
            super(outputStream);
            if (outputStream == null) {
                throw new NullPointerException("Output stream cannot be null");
            }
            this.cpioOutputStreamBuilder = new CpioArchiveOutputStreamBuilder<>(this, outputStream);
        }

        /**
         * Access the CPIO output stream builder for configuration.
         *
         * @return the CPIO output stream builder
         */
        public CpioArchiveOutputStreamBuilder<CpioArchiveCreatorBuilder> cpioOutputStream() {
            return cpioOutputStreamBuilder;
        }

        @Override
        protected CpioArchiveCreatorBuilder getThis() {
            return this;
        }

        @Override
        public CpioArchiveOutputStream buildArchiveOutputStream() throws IOException {
            return cpioOutputStreamBuilder.build();
        }

        @Override
        public CpioArchiveCreator build() throws IOException {
            return new CpioArchiveCreator(this);
        }
    }
}
