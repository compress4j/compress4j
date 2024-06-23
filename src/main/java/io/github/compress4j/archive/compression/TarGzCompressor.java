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
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;

/** The Tar Gz compressor. */
public class TarGzCompressor extends TarCompressor {
    /**
     * Create a new TarGzCompressor with the given file.
     *
     * @param path the file to write the archive to
     * @throws IOException if an I/O error occurred
     */
    public TarGzCompressor(Path path) throws IOException {
        super(path);
    }

    /**
     * Create a new TarGzCompressor with the given file and options
     *
     * @param file the file to write the archive to
     * @param options the options for the compressor
     * @throws IOException if an I/O error occurred
     */
    public TarGzCompressor(Path file, Map<String, Object> options) throws IOException {
        super(Files.newOutputStream(file), options);
    }

    /**
     * Create a new TarGzCompressor with the given output stream.
     *
     * @param stream the output stream to write the archive to
     * @throws IOException if an I/O error occurred
     */
    public TarGzCompressor(OutputStream stream) throws IOException {
        super(stream);
    }

    /**
     * Create a new TarGzCompressor with the given output stream and options.
     *
     * @param stream the output stream to write the archive to
     * @param options the options for the compressor
     * @throws IOException if an I/O error occurred
     */
    public TarGzCompressor(OutputStream stream, Map<String, Object> options) throws IOException {
        super(stream, options);
    }

    /**
     * {@inheritDoc}
     *
     * @param outputStream {@inheritDoc}
     * @param options {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected TarArchiveOutputStream buildArchiveOutputStream(OutputStream outputStream, Map<String, Object> options)
            throws IOException {
        GzipParameters parameters = new GzipParameters();
        parameters.setCompressionLevel(getCompressionLevel(options));

        Map<String, Object> optionsWithoutCompression = options.entrySet().stream()
                .filter(e -> !e.getKey().equals(COMPRESSION_LEVEL))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return super.buildArchiveOutputStream(
                new GzipCompressorOutputStream(outputStream, parameters), optionsWithoutCompression);
    }
}
