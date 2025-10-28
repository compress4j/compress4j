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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XZDecompressorTest {

    private static final String TEST_DATA = "Final XZ Decompressor Test!";
    private byte[] compressedData;
    private Path compressedFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        var outputStream = new ByteArrayOutputStream();
        try (var xzOut = new XZCompressorOutputStream(outputStream)) {
            xzOut.write(TEST_DATA.getBytes(StandardCharsets.UTF_8));
        }
        compressedData = outputStream.toByteArray();

        compressedFile = tempDir.resolve("test.xz");
        Files.write(compressedFile, compressedData);
    }

    @Test
    void testConstructorWithInputStream() throws IOException {
        // Given
        var xzInMock = mock(XZCompressorInputStream.class);

        // When & Then
        try (var decompressor = new XZDecompressor(xzInMock)) {
            assertThat(decompressor).isNotNull();
        }
        verify(xzInMock).close();
    }

    @Test
    void testConstructorWithBuilder() throws IOException {
        // Given
        var in = new ByteArrayInputStream(compressedData);

        // When
        var builder = XZDecompressor.builder(in);

        // Then
        try (var decompressor = new XZDecompressor(builder)) {
            assertThat(decompressor).isNotNull();
            var tempFile = tempDir.resolve("constructorBuilder.txt");
            decompressor.write(tempFile);
            assertThat(Files.readAllBytes(tempFile)).isEqualTo(TEST_DATA.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void testBuilderWithInputStreamFactory() throws IOException {
        // Given
        var in = new ByteArrayInputStream(compressedData);

        // When
        var builder = XZDecompressor.builder(in);

        // Then
        assertThat(builder).isNotNull();
        try (var decompressor = builder.build()) {
            assertThat(decompressor).isNotNull();
        }
    }

    @Test
    void testBuilderWithPathFactory() throws IOException {
        // When
        var builder = XZDecompressor.builder(compressedFile);

        // Then
        assertThat(builder).isNotNull();
        try (var decompressor = builder.build()) {
            assertThat(decompressor).isNotNull();
        }
    }

    @Test
    void testDecompressToOutputStream() throws IOException {
        // When & Then
        try (var decompressor = XZDecompressor.builder(compressedFile).build()) {
            var tempFile = tempDir.resolve("decompressToStream.txt");
            decompressor.write(tempFile);
            assertThat(Files.readAllBytes(tempFile)).isEqualTo(TEST_DATA.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void testDecompressToPath() throws IOException {
        // Given
        var decompressedFile = tempDir.resolve("decompress.txt");

        // When
        try (var decompressor = XZDecompressor.builder(compressedFile).build()) {
            decompressor.write(decompressedFile);
        }

        // Then
        assertThat(Files.readAllBytes(decompressedFile)).isEqualTo(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testDecompressToFile() throws IOException {
        // Given
        var decompressedFile = tempDir.resolve("decompress.txt").toFile();

        // When
        try (var decompressor = XZDecompressor.builder(compressedFile).build()) {
            decompressor.write(decompressedFile);
        }

        // Then
        assertThat(Files.readAllBytes(decompressedFile.toPath())).isEqualTo(TEST_DATA.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testDecompressWithBuilderOptions() throws IOException {
        // When
        try (var decompressor = XZDecompressor.builder(compressedFile)
                .compressorInputStreamBuilder()
                // Set limit to 10MB (10240 KiB), which is > 8,296 KiB required by default compression
                .setMemoryLimitInKb(10240)
                .setDecompressConcatenated(true)
                .parentBuilder()
                .build()) {

            var tempFile = tempDir.resolve("builderOptions.txt");
            decompressor.write(tempFile);

            // Then
            assertThat(Files.readAllBytes(tempFile)).isEqualTo(TEST_DATA.getBytes(StandardCharsets.UTF_8));
        }
    }
}
