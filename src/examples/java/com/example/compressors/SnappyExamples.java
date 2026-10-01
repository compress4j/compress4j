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

import io.github.compress4j.compressors.snappy.SnappyFramedCompressor;
import io.github.compress4j.compressors.snappy.SnappyFramedDecompressor;
import io.github.compress4j.compressors.snappy.SnappyRawCompressor;
import io.github.compress4j.compressors.snappy.SnappyRawDecompressor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings({"java:S1192", "unused"})
public class SnappyExamples {
    private SnappyExamples() {
        /* no-op */
    }

    /** Example for framed Snappy compression. */
    public static void framedCompressor() throws IOException {
        // tag::snappy-framed-compressor[]
        try (SnappyFramedCompressor snappyCompressor =
                SnappyFramedCompressor.builder(Path.of("example.sz")).build()) {
            snappyCompressor.write(Path.of("path/to/file.txt"));
        }
        // end::snappy-framed-compressor[]
    }

    /** Example for framed Snappy decompression. */
    public static void framedDecompressor() throws IOException {
        // tag::snappy-framed-decompressor[]
        try (SnappyFramedDecompressor snappyDecompressor =
                SnappyFramedDecompressor.builder(Path.of("example.sz")).build()) {
            snappyDecompressor.write(Path.of("path/to/file.txt"));
        }
        // end::snappy-framed-decompressor[]
    }

    /** Example for raw Snappy compression, which needs the uncompressed length up front. */
    public static void rawCompressor() throws IOException {
        // tag::snappy-raw-compressor[]
        Path source = Path.of("path/to/file.txt");
        try (SnappyRawCompressor snappyCompressor = SnappyRawCompressor.builder(
                        Path.of("example.snappy"), Files.size(source))
                .build()) {
            snappyCompressor.write(source);
        }
        // end::snappy-raw-compressor[]
    }

    /** Example for raw Snappy decompression. */
    public static void rawDecompressor() throws IOException {
        // tag::snappy-raw-decompressor[]
        try (SnappyRawDecompressor snappyDecompressor =
                SnappyRawDecompressor.builder(Path.of("example.snappy")).build()) {
            snappyDecompressor.write(Path.of("path/to/file.txt"));
        }
        // end::snappy-raw-decompressor[]
    }
}
