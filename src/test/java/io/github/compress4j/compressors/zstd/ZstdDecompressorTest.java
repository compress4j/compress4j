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
package io.github.compress4j.compressors.zstd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.compress4j.compressors.zstd.ZstdDecompressor.ZstdDecompressorBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZstdDecompressorTest {

    private static final String CONTENT = "Zstd decompression test. ".repeat(100);

    @TempDir
    Path tempDir;

    private Path compressedFile() throws IOException {
        var source = tempDir.resolve("source.txt");
        Files.writeString(source, CONTENT, StandardCharsets.UTF_8);
        var compressed = tempDir.resolve("source.txt.zst");
        try (var compressor = ZstdCompressor.builder(compressed).build()) {
            compressor.write(source);
        }
        return compressed;
    }

    @Test
    void shouldDecompressFromPath() throws IOException {
        var output = tempDir.resolve("out.txt");

        try (var decompressor = ZstdDecompressor.builder(compressedFile()).build()) {
            decompressor.write(output);
        }

        assertThat(output).hasContent(CONTENT);
    }

    @Test
    void shouldDecompressFromStream() throws IOException {
        var output = tempDir.resolve("out.txt");

        try (var decompressor =
                ZstdDecompressor.builder(Files.newInputStream(compressedFile())).build()) {
            decompressor.write(output);
        }

        assertThat(output).hasContent(CONTENT);
    }

    @Test
    void shouldDecompressFromFile() throws IOException {
        var output = tempDir.resolve("out.txt");

        try (var decompressor = new ZstdDecompressorBuilder(compressedFile().toFile()).build()) {
            decompressor.write(output);
        }

        assertThat(output).hasContent(CONTENT);
    }

    @Test
    void shouldExposeInputStreamBuilderParent() throws IOException {
        try (var in = Files.newInputStream(compressedFile())) {
            var builder = ZstdDecompressor.builder(in);

            assertThat(builder.compressorInputStreamBuilder().parentBuilder()).isSameAs(builder);
        }
    }

    @Test
    void shouldRejectNonZstdInput() throws IOException {
        var gzip = new ByteArrayOutputStream();
        try (var out = new GZIPOutputStream(gzip)) {
            out.write(CONTENT.getBytes(StandardCharsets.UTF_8));
        }

        var output = tempDir.resolve("out.txt");

        try (var decompressor = ZstdDecompressor.builder(new ByteArrayInputStream(gzip.toByteArray()))
                .build()) {
            assertThatThrownBy(() -> decompressor.write(output)).isInstanceOf(IOException.class);
        }
    }
}
