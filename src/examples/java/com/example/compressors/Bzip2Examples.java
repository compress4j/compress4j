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

import io.github.compress4j.compressors.bzip2.BZip2Compressor;
import io.github.compress4j.compressors.bzip2.BZip2Decompressor;
import java.io.IOException;
import java.nio.file.Path;

@SuppressWarnings({"unused"})
public class Bzip2Examples {
    private Bzip2Examples() {
        // Usage example
    }

    public static void compressor() throws IOException {
        // tag::bzip2-compressor[]
        try (BZip2Compressor bzip2Compressor = BZip2Compressor.builder(Path.of("example.bz2"))
                .compressorOutputStreamBuilder()
                .blockSize(5)
                .parentBuilder()
                .build()) {
            bzip2Compressor.write(Path.of("path/to/file.txt"));
        }
        // end::bzip2-compressor[]
    }

    public static void decompressor() throws IOException {
        // tag::bzip2-decompressor[]
        try (BZip2Decompressor gzipDecompressor =
                BZip2Decompressor.builder(Path.of("example.bz2")).build()) {
            gzipDecompressor.write(Path.of("path/to/file.txt"));
        }
        // end::bzip2-decompressor[]
    }
}
