/*
 * Copyright 2024 The Compress4J Project
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
import java.util.Collections;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;

public class TarGzCompressor extends TarCompressor {
    public TarGzCompressor(Path path) throws IOException {
        this(path, Collections.emptyMap());
    }

    public TarGzCompressor(Path file, Map<String, Object> options) throws IOException {
        this(Files.newOutputStream(file), options);
    }

    public TarGzCompressor(OutputStream stream) throws IOException {
        this(stream, Collections.emptyMap());
    }

    public TarGzCompressor(OutputStream stream, Map<String, Object> options) throws IOException {
        super(stream, options);
    }

    /** {@inheritDoc} */
    @Override
    protected TarArchiveOutputStream createArchiveOutputStream(OutputStream s) throws IOException {
        return createArchiveOutputStream(s, Collections.emptyMap());
    }

    /** {@inheritDoc} */
    @Override
    protected TarArchiveOutputStream createArchiveOutputStream(OutputStream stream, Map<String, Object> options)
            throws IOException {
        GzipCompressorOutputStream out;
        int compressionLevel = getCompressionLevel(options);
        if (compressionLevel != -1) {
            GzipParameters parameters = new GzipParameters();
            parameters.setCompressionLevel(compressionLevel);
            out = new GzipCompressorOutputStream(stream, parameters);
        } else {
            out = new GzipCompressorOutputStream(stream);
        }
        return super.createArchiveOutputStream(out, options);
    }
}
