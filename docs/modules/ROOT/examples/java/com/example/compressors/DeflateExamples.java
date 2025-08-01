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
package com.example.compressors;

import io.github.compress4j.compressors.deflate.DeflateCompressionLevel;
import io.github.compress4j.compressors.deflate.DeflateCompressor;
import io.github.compress4j.compressors.deflate.DeflateDecompressor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"unused"})
public class DeflateExamples {
    private DeflateExamples() {
        // Usage example
    }

    public static void compressor() throws IOException {
        // tag::deflate-compressor[]
        try (DeflateCompressor deflateCompressor = DeflateCompressor.builder(Path.of("example.deflate"))
                .compressorOutputStreamBuilder()
                .setCompressionLevel(DeflateCompressionLevel.BEST_COMPRESSION)
                .setZlibHeader(true)
                .parentBuilder()
                .build()) {
            deflateCompressor.write(Path.of("path/to/file.txt"));
        }
        // end::deflate-compressor[]
    }

    public static void decompressor() throws IOException {
        // tag::deflate-decompressor[]
        try (DeflateDecompressor gzipDecompressor =
                DeflateDecompressor.builder(Path.of("example.deflate")).build()) {
            gzipDecompressor.write(Path.of("path/to/file.txt"));
        }
        // end::deflate-decompressor[]
    }
}
