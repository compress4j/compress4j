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
package io.github.compress4j.compressors.lz4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.compress4j.compressors.lz4.Lz4BlockCompressor.Lz4BlockCompressorBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Lz4BlockCompressorTest {

    @TempDir
    Path tempDir;

    private Path payload() throws IOException {
        return Files.writeString(tempDir.resolve("payload.txt"), "payload");
    }

    @Test
    void shouldConstructWithStream() throws IOException {
        var mockStream = mock(BlockLZ4CompressorOutputStream.class);

        try (var compressor = new Lz4BlockCompressor(mockStream)) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldConstructWithBuilder() throws IOException {
        var builder = new Lz4BlockCompressorBuilder(new ByteArrayOutputStream());

        try (var compressor = new Lz4BlockCompressor(builder)) {
            compressor.write(payload());
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldBuildWithPath() throws IOException {
        var compressedFile = tempDir.resolve("test.block_lz4");

        try (var compressor = Lz4BlockCompressor.builder(compressedFile).build()) {
            compressor.write(payload());
            assertThat(compressor).isNotNull();
        }
        assertThat(compressedFile).exists();
    }

    @Test
    void shouldRejectDirectoryPath() {
        assertThatThrownBy(() -> Lz4BlockCompressor.builder(tempDir)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldBuildWithOutputStream() throws IOException {
        try (var compressor =
                Lz4BlockCompressor.builder(new ByteArrayOutputStream()).build()) {
            compressor.write(payload());
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldCompressFileReadableByCommonsCompress() throws IOException {
        var original = "block LZ4 test data. ".repeat(200);
        var sourceFile = tempDir.resolve("data.txt");
        var compressedFile = tempDir.resolve("data.txt.block_lz4");
        Files.writeString(sourceFile, original, StandardCharsets.UTF_8);

        try (var compressor = Lz4BlockCompressor.builder(compressedFile).build()) {
            compressor.write(sourceFile);
        }

        assertThat(Files.size(compressedFile)).isPositive().isLessThan(original.length());
        try (var in = new BlockLZ4CompressorInputStream(Files.newInputStream(compressedFile))) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(original);
        }
    }
}
