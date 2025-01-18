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

import static io.github.compress4j.archive.compression.Compressor.COMPRESSION_LEVEL;
import static io.github.compress4j.archive.compression.Compressor.getCompressionLevel;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;

public class TarGzArchiveOutputStreamBuilder extends TarArchiveOutputStreamBuilder {
    private final UnaryOperator<Map<String, Object>> removeCompression = options -> options.entrySet().stream()
            .filter(e -> !e.getKey().equals(COMPRESSION_LEVEL))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    /**
     * Create a new TarGzArchiveOutputStreamBuilder with the given path.
     *
     * @param path the path to write the archive to
     * @throws IOException if an I/O error occurred
     */
    public TarGzArchiveOutputStreamBuilder(Path path) throws IOException {
        super(path);
    }

    /**
     * Create a new TarGzArchiveOutputStreamBuilder with the given path and options.
     *
     * @param path the path to write the archive to
     * @param options the options for the compressor
     * @throws IOException if an I/O error occurred
     */
    public TarGzArchiveOutputStreamBuilder(Path path, Map<String, Object> options) throws IOException {
        super(path, options);
    }

    /**
     * Create a new TarGzArchiveOutputStreamBuilder with the given output stream.
     *
     * @param outputStream the output stream
     */
    public TarGzArchiveOutputStreamBuilder(OutputStream outputStream) {
        super(outputStream);
    }

    /**
     * Create a new TarGzArchiveOutputStreamBuilder with the given output stream and options.
     *
     * @param outputStream the output outputStream
     * @param options the options for the compressor
     */
    public TarGzArchiveOutputStreamBuilder(OutputStream outputStream, Map<String, Object> options) {
        super(outputStream, options);
    }

    /** {@inheritDoc} */
    @Override
    public TarArchiveOutputStream build() throws IOException {
        GzipParameters parameters = new GzipParameters();
        parameters.setCompressionLevel(getCompressionLevel(options));

        return super.buildTarArchiveOutputStream(
                new GzipCompressorOutputStream(outputStream, parameters), removeCompression.apply(options));
    }
}
