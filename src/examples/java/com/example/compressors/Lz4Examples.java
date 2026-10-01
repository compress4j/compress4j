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

import io.github.compress4j.compressors.lz4.Lz4BlockCompressor;
import io.github.compress4j.compressors.lz4.Lz4BlockDecompressor;
import io.github.compress4j.compressors.lz4.Lz4FramedCompressor;
import io.github.compress4j.compressors.lz4.Lz4FramedDecompressor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"unused"})
public class Lz4Examples {
    private Lz4Examples() {
        /* no-op */
    }

    /** Example for framed LZ4 compression. */
    public static void framedCompressor() throws IOException {
        // tag::lz4-framed-compressor[]
        try (Lz4FramedCompressor lz4Compressor =
                Lz4FramedCompressor.builder(Path.of("example.lz4")).build()) {
            lz4Compressor.write(Path.of("path/to/file.txt"));
        }
        // end::lz4-framed-compressor[]
    }

    /** Example for framed LZ4 decompression. */
    public static void framedDecompressor() throws IOException {
        // tag::lz4-framed-decompressor[]
        try (Lz4FramedDecompressor lz4Decompressor = Lz4FramedDecompressor.builder(Path.of("example.lz4"))
                .compressorInputStreamBuilder()
                .setDecompressConcatenated(true)
                .parentBuilder()
                .build()) {
            lz4Decompressor.write(Path.of("path/to/file.txt"));
        }
        // end::lz4-framed-decompressor[]
    }

    /** Example for block LZ4 compression. */
    public static void blockCompressor() throws IOException {
        // tag::lz4-block-compressor[]
        try (Lz4BlockCompressor lz4Compressor =
                Lz4BlockCompressor.builder(Path.of("example.block_lz4")).build()) {
            lz4Compressor.write(Path.of("path/to/file.txt"));
        }
        // end::lz4-block-compressor[]
    }

    /** Example for block LZ4 decompression. */
    public static void blockDecompressor() throws IOException {
        // tag::lz4-block-decompressor[]
        try (Lz4BlockDecompressor lz4Decompressor =
                Lz4BlockDecompressor.builder(Path.of("example.block_lz4")).build()) {
            lz4Decompressor.write(Path.of("path/to/file.txt"));
        }
        // end::lz4-block-decompressor[]
    }
}
