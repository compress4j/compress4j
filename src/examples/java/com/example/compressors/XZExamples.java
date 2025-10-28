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

import io.github.compress4j.compressors.xz.XZCompressor;
import io.github.compress4j.compressors.xz.XZDecompressor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"unused"})
public class XZExamples {
    private XZExamples() {
        // Usage example
    }

    /** Example for XZ compression using builder pattern. */
    public static void compressor() throws IOException {
        // tag::xz-compressor[]
        try (XZCompressor xzCompressor = XZCompressor.builder(Path.of("example.xz"))
                .compressorOutputStreamBuilder()
                .preset(6) // Set preset level (0-9)
                .parentBuilder()
                .build()) {
            xzCompressor.write(Path.of("path/to/file.txt"));
        }
        // end::xz-compressor[]
    }

    /** Example for XZ decompression using builder pattern. */
    public static void decompressor() throws IOException {
        // tag::xz-decompressor[]
        try (XZDecompressor xzDecompressor = XZDecompressor.builder(Path.of("example.xz"))
                .compressorInputStreamBuilder()
                .setDecompressConcatenated(true)
                .parentBuilder()
                .build()) {
            xzDecompressor.write(Path.of("path/to/file.txt"));
        }
        // end::xz-decompressor[]
    }
}
