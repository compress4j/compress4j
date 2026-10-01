/*
 * Copyright 2026 The Compress4J Project
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
package com.example.compressors;

import io.github.compress4j.compressors.zstd.ZstdCompressor;
import io.github.compress4j.compressors.zstd.ZstdDecompressor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"unused"})
public class ZstdExamples {
    private ZstdExamples() {
        /* no-op */
    }

    /** Example for Zstandard compression using builder pattern. */
    public static void compressor() throws IOException {
        // tag::zstd-compressor[]
        try (ZstdCompressor zstdCompressor = ZstdCompressor.builder(Path.of("example.zst"))
                .compressorOutputStreamBuilder()
                .level(6)
                .parentBuilder()
                .build()) {
            zstdCompressor.write(Path.of("path/to/file.txt"));
        }
        // end::zstd-compressor[]
    }

    /** Example for Zstandard decompression using builder pattern. */
    public static void decompressor() throws IOException {
        // tag::zstd-decompressor[]
        try (ZstdDecompressor zstdDecompressor =
                ZstdDecompressor.builder(Path.of("example.zst")).build()) {
            zstdDecompressor.write(Path.of("path/to/file.txt"));
        }
        // end::zstd-decompressor[]
    }
}
