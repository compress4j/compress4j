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
package com.hominux.compress4j.compressors.zstd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.hominux.compress4j.compressors.zstd.ZstdCompressor.ZstdCompressorBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZstdCompressorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldConstructWithStream() throws IOException {
        var mockStream = mock(ZstdCompressorOutputStream.class);

        try (var compressor = new ZstdCompressor(mockStream)) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldConstructWithBuilder() throws IOException {
        var builder = new ZstdCompressorBuilder(new ByteArrayOutputStream());

        try (var compressor = new ZstdCompressor(builder)) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldBuildWithPath() throws IOException {
        var compressedFile = tempDir.resolve("test.zst");

        try (var compressor = ZstdCompressor.builder(compressedFile).build()) {
            assertThat(compressor).isNotNull();
        }
        assertThat(compressedFile).exists();
    }

    @Test
    void shouldRejectDirectoryPath() {
        assertThatThrownBy(() -> ZstdCompressor.builder(tempDir)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldBuildWithOutputStream() throws IOException {
        try (var compressor =
                ZstdCompressor.builder(new ByteArrayOutputStream()).build()) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldAcceptLevelBoundary() {
        var builder = ZstdCompressor.builder(new ByteArrayOutputStream());

        assertThat(builder.compressorOutputStreamBuilder().level(22)).isNotNull();
    }

    @Test
    void shouldRejectLevelAboveMaximum() {
        var streamBuilder = ZstdCompressor.builder(new ByteArrayOutputStream()).compressorOutputStreamBuilder();

        assertThatThrownBy(() -> streamBuilder.level(23))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Zstd level must be in the range")
                .hasMessageContaining("but was: 23");
    }

    @Test
    void shouldRejectLevelBelowMinimum() {
        var streamBuilder = ZstdCompressor.builder(new ByteArrayOutputStream()).compressorOutputStreamBuilder();

        assertThatThrownBy(() -> streamBuilder.level(Integer.MIN_VALUE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCompressFileReadableByCommonsCompress() throws IOException {
        var original = "Zstd test data. ".repeat(200);
        var sourceFile = tempDir.resolve("data.txt");
        var compressedFile = tempDir.resolve("data.txt.zst");
        Files.writeString(sourceFile, original, StandardCharsets.UTF_8);

        try (var compressor = ZstdCompressor.builder(compressedFile)
                .compressorOutputStreamBuilder()
                .level(5)
                .parentBuilder()
                .build()) {
            compressor.write(sourceFile);
        }

        assertThat(Files.size(compressedFile)).isPositive().isLessThan(original.length());
        try (var in = new ZstdCompressorInputStream(Files.newInputStream(compressedFile))) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(original);
        }
    }
}
