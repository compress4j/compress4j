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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.io.IOUtils;

/** The Tar compressor. */
@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
public class TarCompressor extends Compressor<TarArchiveOutputStream> {
    private final TarArchiveOutputStream tarArchiveOutputStream;

    /**
     * Create a new TarCompressor with the given file.
     *
     * @param path the file to write the archive to
     * @throws IOException if an I/O error occurred
     */
    public TarCompressor(Path path) throws IOException {
        this(path, Collections.emptyMap());
    }

    /**
     * Create a new TarCompressor with the given file and options.
     *
     * @param file the file to write the archive to
     * @param options the options for the compressor
     * @throws IOException if an I/O error occurred
     */
    public TarCompressor(Path file, Map<String, Object> options) throws IOException {
        this(Files.newOutputStream(file), options);
    }

    /**
     * Create a new TarCompressor with the given output stream.
     *
     * @param stream the output stream
     * @throws IOException if an I/O error occurred
     */
    public TarCompressor(OutputStream stream) throws IOException {
        tarArchiveOutputStream = createArchiveOutputStream(stream);
    }

    /**
     * Create a new TarCompressor with the given output stream and options.
     *
     * @param stream the output stream
     * @param options the options for the compressor
     * @throws IOException if an I/O error occurred
     */
    public TarCompressor(OutputStream stream, Map<String, Object> options) throws IOException {
        tarArchiveOutputStream = createArchiveOutputStream(stream, options);
    }

    /** {@inheritDoc} */
    private static TarArchiveEntry getArchiveEntry(String name, Optional<Path> symlinkTarget) {
        return symlinkTarget
                .map(link -> {
                    var entry = new TarArchiveEntry(name, TarConstants.LF_SYMLINK);
                    entry.setSize(0);
                    entry.setLinkName(link.toString());
                    return entry;
                })
                .orElseGet(() -> new TarArchiveEntry(name));
    }

    /** {@inheritDoc} */
    @Override
    protected void writeDirectoryEntry(String name, FileTime modTime) throws IOException {
        TarArchiveEntry e = new TarArchiveEntry(name + '/');
        e.setModTime(modTime);
        tarArchiveOutputStream.putArchiveEntry(e);
        tarArchiveOutputStream.closeArchiveEntry();
    }

    /**
     * Write a file entry to the archive.
     *
     * @param name name of the entry
     * @param source input stream to read the file from
     * @param length length of the file
     * @param modTime last modification time of the file
     * @param mode file mode
     * @param symlinkTarget target of the symbolic link, or {@code null} if the entry is not a symbolic link
     * @throws IOException if an I/O error occurred
     */
    protected void writeFileEntry(
            String name, InputStream source, long length, FileTime modTime, int mode, Optional<Path> symlinkTarget)
            throws IOException {
        TarArchiveEntry e = getArchiveEntry(name, symlinkTarget);
        if (length < 0) {
            length = source.available();
        }
        if (symlinkTarget.isEmpty()) {
            e.setSize(length);
        }
        e.setModTime(modTime);
        if (mode != 0) {
            e.setMode(mode);
        }
        tarArchiveOutputStream.putArchiveEntry(e);
        if (length > 0) {
            IOUtils.copy(source, tarArchiveOutputStream);
        }
        tarArchiveOutputStream.closeArchiveEntry();
    }

    /** {@inheritDoc} */
    @Override
    protected TarArchiveOutputStream createArchiveOutputStream(OutputStream stream, Map<String, Object> options)
            throws IOException {
        TarArchiveOutputStream out = new TarArchiveOutputStream(stream, UTF_8.name());
        out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        return applyFormatOptions(out, options);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        tarArchiveOutputStream.close();
    }
}
