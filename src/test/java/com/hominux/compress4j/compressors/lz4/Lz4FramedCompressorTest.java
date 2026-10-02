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
import static org.mockito.Mockito.mock;

import com.hominux.compress4j.compressors.lz4.Lz4FramedCompressor.Lz4FramedCompressorBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Lz4FramedCompressorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldConstructWithStream() throws IOException {
        var mockStream = mock(FramedLZ4CompressorOutputStream.class);

        try (var compressor = new Lz4FramedCompressor(mockStream)) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldConstructWithBuilder() throws IOException {
        var builder = new Lz4FramedCompressorBuilder(new ByteArrayOutputStream());

        try (var compressor = new Lz4FramedCompressor(builder)) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldBuildWithPath() throws IOException {
        var compressedFile = tempDir.resolve("test.lz4");

        try (var compressor = Lz4FramedCompressor.builder(compressedFile).build()) {
            assertThat(compressor).isNotNull();
        }
        assertThat(compressedFile).exists();
    }

    @Test
    void shouldRejectDirectoryPath() {
        assertThatThrownBy(() -> Lz4FramedCompressor.builder(tempDir)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldBuildWithOutputStream() throws IOException {
        try (var compressor =
                Lz4FramedCompressor.builder(new ByteArrayOutputStream()).build()) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldCompressFileReadableByCommonsCompress() throws IOException {
        var original = "framed LZ4 test data. ".repeat(200);
        var sourceFile = tempDir.resolve("data.txt");
        var compressedFile = tempDir.resolve("data.txt.lz4");
        Files.writeString(sourceFile, original, StandardCharsets.UTF_8);

        try (var compressor = Lz4FramedCompressor.builder(compressedFile).build()) {
            compressor.write(sourceFile);
        }

        assertThat(Files.size(compressedFile)).isPositive().isLessThan(original.length());
        try (var in = new FramedLZ4CompressorInputStream(Files.newInputStream(compressedFile))) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(original);
        }
    }
}
