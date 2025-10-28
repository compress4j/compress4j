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
package io.github.compress4j.compressors.xz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.compress4j.compressors.xz.XZCompressor.XZCompressorBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tukaani.xz.XZInputStream;

/** Tests for the {@link XZCompressor} class. */
class XZCompressorTest {

    @TempDir
    Path tempDir;

    @Test
    void testConstructorWithStream() throws IOException {
        // Given
        var mockStream = mock(XZCompressorOutputStream.class);

        // When
        try (var compressor = new XZCompressor(mockStream)) {
            // Then
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void testConstructorWithBuilder() throws IOException {
        // Given
        var outputStream = new ByteArrayOutputStream();
        var builder = new XZCompressorBuilder(outputStream);

        // When
        try (var compressor = new XZCompressor(builder)) {
            // Then
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void testStaticBuilderWithPath() throws IOException {
        // Given
        var compressedFile = tempDir.resolve("test.xz");

        // When
        try (var compressor = XZCompressor.builder(compressedFile).build()) {
            // Then
            assertThat(compressor).isNotNull();
            assertThat(compressedFile).exists();
        }
    }

    @Test
    void testStaticBuilderWithPathThrowsExceptionForDirectory() {
        // When & Then
        assertThatThrownBy(() -> XZCompressor.builder(tempDir)).isInstanceOf(IOException.class);
    }

    @Test
    void testStaticBuilderWithNullPath() {
        // Given
        Path path = null;

        // When & Then
        //noinspection ConstantValue
        assertThatThrownBy(() -> XZCompressor.builder(path)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testStaticBuilderWithOutputStream() throws IOException {
        // Given
        var outputStream = new ByteArrayOutputStream();

        // When
        try (var compressor = XZCompressor.builder(outputStream).build()) {
            // Then
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void testFullCompressionLifecycle() throws IOException {
        // iven
        var originalData = "Test data for XZ compression. "
                + "This string will be compressed and then decompressed. "
                + "Repeating data helps compression. ".repeat(20);
        var compressedFile = tempDir.resolve("data.txt.xz");
        var sourceFile = tempDir.resolve("data.txt");
        Files.writeString(sourceFile, originalData, StandardCharsets.UTF_8);

        // When
        try (var xzCompressor = XZCompressor.builder(compressedFile)
                .compressorOutputStreamBuilder()
                .preset(1)
                .parentBuilder()
                .build()) {

            xzCompressor.write(sourceFile);
        }

        // Then
        assertThat(compressedFile).exists();
        assertThat(Files.size(compressedFile)).isGreaterThan(0L);
        assertThat(Files.size(compressedFile)).isLessThan(originalData.getBytes(StandardCharsets.UTF_8).length);

        try (var fi = Files.newInputStream(compressedFile);
                var xzIn = new XZInputStream(fi)) {

            byte[] uncompressedData = xzIn.readAllBytes();
            var decompressedData = new String(uncompressedData, StandardCharsets.UTF_8);

            assertThat(decompressedData).isEqualTo(originalData);
        }
    }
}
