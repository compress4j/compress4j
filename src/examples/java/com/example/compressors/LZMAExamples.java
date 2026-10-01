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

import io.github.compress4j.compressors.lzma.LZMACompressor;
import io.github.compress4j.compressors.lzma.LZMADecompressor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"unused"})
public class LZMAExamples {
    private LZMAExamples() {
        /* no-op */
    }

    /** Example for LZMA compression using builder pattern. */
    public static void compressor() throws IOException {
        // tag::lzma-compressor[]
        try (LZMACompressor lzmaCompressor =
                LZMACompressor.builder(Path.of("example.lzma")).build()) {
            lzmaCompressor.write(Path.of("path/to/file.txt"));
        }
        // end::lzma-compressor[]
    }

    /** Example for LZMA decompression using builder pattern. */
    public static void decompressor() throws IOException {
        // tag::lzma-decompressor[]
        try (LZMADecompressor lzmaDecompressor = LZMADecompressor.builder(Path.of("example.lzma"))
                .compressorInputStreamBuilder()
                .setMemoryLimitInKb(65536)
                .parentBuilder()
                .build()) {
            lzmaDecompressor.write(Path.of("path/to/file.txt"));
        }
        // end::lzma-decompressor[]
    }
}
