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

import io.github.compress4j.archive.compression.builder.TarArchiveOutputStreamBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.io.IOUtils;

/** The Tar compressor. */
public class TarCompressor extends Compressor<TarArchiveOutputStream> {

    /**
     * Create a new TarCompressor with the given output stream.
     *
     * @param tarArchiveOutputStream the output Tar Archive Output Stream
     * @throws IOException if an I/O error occurred
     */
    public TarCompressor(TarArchiveOutputStream tarArchiveOutputStream) throws IOException {
        super(tarArchiveOutputStream);
    }

    /**
     * Create a new TarCompressor with the given output stream and options.
     *
     * @param archiveOutputStreamBuilder the archive output stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarCompressor(TarArchiveOutputStreamBuilder archiveOutputStreamBuilder) throws IOException {
        super(archiveOutputStreamBuilder);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeDirectoryEntry(String name, FileTime modTime) throws IOException {
        TarArchiveEntry e = new TarArchiveEntry(name + '/');
        e.setModTime(modTime);
        archiveOutputStream.putArchiveEntry(e);
        archiveOutputStream.closeArchiveEntry();
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
        archiveOutputStream.putArchiveEntry(e);
        if (length > 0) {
            IOUtils.copy(source, archiveOutputStream);
        }
        archiveOutputStream.closeArchiveEntry();
    }

    private static TarArchiveEntry getArchiveEntry(
            String name, @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Path> symlinkTarget) {
        return symlinkTarget
                .map(link -> {
                    var entry = new TarArchiveEntry(name, TarConstants.LF_SYMLINK);
                    entry.setSize(0);
                    entry.setLinkName(link.toString());
                    return entry;
                })
                .orElseGet(() -> new TarArchiveEntry(name));
    }
}
