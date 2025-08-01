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
