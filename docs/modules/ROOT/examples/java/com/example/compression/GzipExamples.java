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
package com.example.compression;

import static java.util.zip.Deflater.BEST_COMPRESSION;
import static java.util.zip.Deflater.HUFFMAN_ONLY;

import io.github.compress4j.compression.gzip.GzipCompressor;
import java.nio.file.Path;

@SuppressWarnings("unused")
public class GzipExamples {
    private GzipExamples() {
        // Usage example
    }

    public static void compressor() throws Exception {
        // tag::gzip-compressor[]
        try (GzipCompressor gzipCompressor = GzipCompressor.builder(Path.of("example.gz"))
                .compressorOutputStreamBuilder()
                .bufferSize(1024)
                .compressionLevel(BEST_COMPRESSION)
                .comment("comment")
                .deflateStrategy(HUFFMAN_ONLY)
                .operatingSystem(0)
                .parentBuilder()
                .build()) {
            gzipCompressor.write(Path.of("path/to/file.txt"));
        }
        // end::gzip-compressor[]
    }

    public static void decompressor() {
        // tag::gzip-decompressor[]
        // TODO: Implement decompression logic
        // GzipDecompressor gzipDecompressor = GzipDecompressor.builder(inputStream).build();
        // gzipDecompressor.decompress(outputStream);
        // end::gzip-decompressor[]
    }
}
