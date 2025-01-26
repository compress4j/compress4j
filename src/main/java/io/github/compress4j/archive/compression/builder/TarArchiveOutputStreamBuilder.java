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
package io.github.compress4j.archive.compression.builder;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class TarArchiveOutputStreamBuilder extends ArchiveOutputStreamBuilder<TarArchiveOutputStream> {
    /**
     * Create a new TarArchiveOutputStreamBuilder with the given path.
     *
     * @param path the path to write the archive to
     * @throws IOException if an I/O error occurred
     */
    public TarArchiveOutputStreamBuilder(Path path) throws IOException {
        this(Files.newOutputStream(path));
    }

    /**
     * Create a new TarArchiveOutputStreamBuilder with the given path and options.
     *
     * @param path the path to write the archive to
     * @param options the options for the compressor
     * @throws IOException if an I/O error occurred
     */
    public TarArchiveOutputStreamBuilder(Path path, Map<String, Object> options) throws IOException {
        this(Files.newOutputStream(path), options);
    }

    /**
     * Create a new TarArchiveOutputStreamBuilder with the given output stream.
     *
     * @param outputStream the output stream
     */
    public TarArchiveOutputStreamBuilder(OutputStream outputStream) {
        super(outputStream);
    }

    /**
     * Create a new TarArchiveOutputStreamBuilder with the given output stream and options.
     *
     * @param outputStream the output outputStream
     * @param options the options for the compressor
     */
    public TarArchiveOutputStreamBuilder(OutputStream outputStream, Map<String, Object> options) {
        super(outputStream, options);
    }

    /** {@inheritDoc} */
    @Override
    public TarArchiveOutputStream build() throws IOException {
        return buildTarArchiveOutputStream(outputStream, options);
    }

    /**
     * Start a new TarArchiveOutputStream. Entries can be included in the archive using the putEntry method, and then
     * the archive should be closed using its close method. In addition, options can be applied to the underlying
     * stream. E.g. compression level.
     *
     * @param outputStream underlying output stream to which to write the archive.
     * @param options options to apply to the underlying output stream. Keys are option names and values are option
     *     values.
     * @return new archive object for use in putEntry
     * @throws IOException thrown by the underlying output stream for I/O errors
     */
    protected TarArchiveOutputStream buildTarArchiveOutputStream(OutputStream outputStream, Map<String, Object> options)
            throws IOException {
        TarArchiveOutputStream out = new TarArchiveOutputStream(outputStream, UTF_8.name());
        out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
        return applyFormatOptions(out, options);
    }
}
