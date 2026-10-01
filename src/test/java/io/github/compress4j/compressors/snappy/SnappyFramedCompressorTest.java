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
package io.github.compress4j.compressors.snappy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.compress4j.compressors.snappy.SnappyFramedCompressor.SnappyFramedCompressorBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorInputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnappyFramedCompressorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldConstructWithStream() throws IOException {
        var mockStream = mock(FramedSnappyCompressorOutputStream.class);

        try (var compressor = new SnappyFramedCompressor(mockStream)) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldConstructWithBuilder() throws IOException {
        var builder = new SnappyFramedCompressorBuilder(new ByteArrayOutputStream());

        try (var compressor = new SnappyFramedCompressor(builder)) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldBuildWithPath() throws IOException {
        var compressedFile = tempDir.resolve("test.sz");

        try (var compressor = SnappyFramedCompressor.builder(compressedFile).build()) {
            assertThat(compressor).isNotNull();
        }
        assertThat(compressedFile).exists();
    }

    @Test
    void shouldRejectDirectoryPath() {
        assertThatThrownBy(() -> SnappyFramedCompressor.builder(tempDir)).isInstanceOf(IOException.class);
    }

    @Test
    void shouldBuildWithOutputStream() throws IOException {
        try (var compressor =
                SnappyFramedCompressor.builder(new ByteArrayOutputStream()).build()) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void shouldCompressFileReadableByCommonsCompress() throws IOException {
        var original = "framed Snappy test data. ".repeat(200);
        var sourceFile = tempDir.resolve("data.txt");
        var compressedFile = tempDir.resolve("data.txt.sz");
        Files.writeString(sourceFile, original, StandardCharsets.UTF_8);

        try (var compressor = SnappyFramedCompressor.builder(compressedFile).build()) {
            compressor.write(sourceFile);
        }

        assertThat(Files.size(compressedFile)).isPositive().isLessThan(original.length());
        try (var in = new FramedSnappyCompressorInputStream(Files.newInputStream(compressedFile))) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(original);
        }
    }
}
