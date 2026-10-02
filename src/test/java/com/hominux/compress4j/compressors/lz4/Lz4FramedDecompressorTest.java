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
package com.hominux.compress4j.compressors.lz4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hominux.compress4j.compressors.lz4.Lz4FramedDecompressor.Lz4FramedDecompressorBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Lz4FramedDecompressorTest {

    private static final String CONTENT = "framed LZ4 decompression test. ".repeat(100);

    @TempDir
    Path tempDir;

    private Path compressedFile() throws IOException {
        var source = tempDir.resolve("source.txt");
        Files.writeString(source, CONTENT, StandardCharsets.UTF_8);
        var compressed = tempDir.resolve("source.txt.lz4");
        try (var compressor = Lz4FramedCompressor.builder(compressed).build()) {
            compressor.write(source);
        }
        return compressed;
    }

    @Test
    void shouldDecompressFromPath() throws IOException {
        var output = tempDir.resolve("out.txt");

        try (var decompressor = Lz4FramedDecompressor.builder(compressedFile()).build()) {
            decompressor.write(output);
        }

        assertThat(output).hasContent(CONTENT);
    }

    @Test
    void shouldDecompressFromStream() throws IOException {
        var output = tempDir.resolve("out.txt");

        try (var decompressor = Lz4FramedDecompressor.builder(Files.newInputStream(compressedFile()))
                .build()) {
            decompressor.write(output);
        }

        assertThat(output).hasContent(CONTENT);
    }

    @Test
    void shouldDecompressFromFile() throws IOException {
        var output = tempDir.resolve("out.txt");

        try (var decompressor =
                new Lz4FramedDecompressorBuilder(compressedFile().toFile()).build()) {
            decompressor.write(output);
        }

        assertThat(output).hasContent(CONTENT);
    }

    @Test
    void shouldExposeInputStreamBuilderParent() throws IOException {
        try (var in = Files.newInputStream(compressedFile())) {
            var builder = Lz4FramedDecompressor.builder(in);

            assertThat(builder.compressorInputStreamBuilder().parentBuilder()).isSameAs(builder);
        }
    }

    private byte[] decompress(byte[] bytes) throws IOException {
        var output = tempDir.resolve("out.bin");
        try (var decompressor =
                Lz4FramedDecompressor.builder(new ByteArrayInputStream(bytes)).build()) {
            decompressor.write(output);
        }
        return Files.readAllBytes(output);
    }

    @Test
    void shouldRejectNonFormatInput() throws IOException {
        var gzip = new ByteArrayOutputStream();
        try (var out = new GZIPOutputStream(gzip)) {
            out.write(CONTENT.getBytes(StandardCharsets.UTF_8));
        }
        var bytes = gzip.toByteArray();

        assertThatThrownBy(() -> decompress(bytes)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldRejectTruncatedInput() throws IOException {
        var compressed = Files.readAllBytes(compressedFile());
        var truncated = Arrays.copyOf(compressed, compressed.length / 2);

        assertThatThrownBy(() -> decompress(truncated)).isInstanceOf(IOException.class);
    }

    private static byte[] frame(String content) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new FramedLZ4CompressorOutputStream(bytes)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private byte[] decompressFrames(byte[] bytes, boolean concatenated) throws IOException {
        var output = tempDir.resolve("frames-" + concatenated + ".bin");
        var builder = Lz4FramedDecompressor.builder(new ByteArrayInputStream(bytes));
        builder.compressorInputStreamBuilder().setDecompressConcatenated(concatenated);
        try (var decompressor = builder.build()) {
            decompressor.write(output);
        }
        return Files.readAllBytes(output);
    }

    @Test
    void shouldDecompressConcatenatedFramesOnlyWhenRequested() throws IOException {
        var both = new ByteArrayOutputStream();
        both.writeBytes(frame("first;"));
        both.writeBytes(frame("second;"));

        assertThat(decompressFrames(both.toByteArray(), true))
                .asString(StandardCharsets.UTF_8)
                .isEqualTo("first;second;");
        assertThat(decompressFrames(both.toByteArray(), false))
                .asString(StandardCharsets.UTF_8)
                .isEqualTo("first;");
    }
}
