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
package io.github.compress4j.archive;

import io.github.compress4j.archive.compression.TarGzCompressor;
import io.github.compress4j.archive.compression.builder.TarGzArchiveOutputStreamBuilder;
import io.github.compress4j.archive.decompression.TarGzDecompressor;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;

class TarGzCompressorIntegrationTest extends TarCompressorIntegrationTest {

    @Override
    @BeforeEach
    void setup() throws IOException {
        compressFile = tempDir.resolve("test.tar.gz");
        compressor = new TarGzCompressor(new TarGzArchiveOutputStreamBuilder(compressFile));
    }

    @Override
    protected void extract(Path in, Path out) throws IOException {
        try (TarGzDecompressor tarGzDecompressor = new TarGzDecompressor(in)) {
            tarGzDecompressor.extract(out);
        }
    }
}