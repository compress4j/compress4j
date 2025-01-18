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
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/** The Tar Gz compressor. */
public class TarGzCompressor extends TarCompressor {

    /**
     * Create a new TarGzCompressor with the given output stream.
     *
     * @param tarArchiveOutputStream the output Tar Archive Output Stream
     * @throws IOException if an I/O error occurred
     */
    public TarGzCompressor(TarArchiveOutputStream tarArchiveOutputStream) throws IOException {
        super(tarArchiveOutputStream);
    }

    /**
     * Create a new TarGzCompressor with the given output stream and options.
     *
     * @param archiveOutputStreamBuilder the archive output stream builder
     * @throws IOException if an I/O error occurred
     */
    public TarGzCompressor(TarArchiveOutputStreamBuilder archiveOutputStreamBuilder) throws IOException {
        super(archiveOutputStreamBuilder);
    }
}
