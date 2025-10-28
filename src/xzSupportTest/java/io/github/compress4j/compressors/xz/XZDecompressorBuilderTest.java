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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.compressors.xz.XZDecompressor.XZDecompressorBuilder;
import io.github.compress4j.compressors.xz.XZDecompressor.XZDecompressorInputStreamBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XZDecompressorBuilderTest {

    private static final String TEST_DATA = "Hello XZ Builder!";
    private byte[] compressedData;
    private Path compressedFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        var outputStream = new ByteArrayOutputStream();
        try (var xzOut = new XZCompressorOutputStream(outputStream)) {
            xzOut.write(TEST_DATA.getBytes(UTF_8));
        }
        compressedData = outputStream.toByteArray();

        compressedFile = tempDir.resolve("test.xz");
        Files.write(compressedFile, compressedData);
    }

    @Test
    void testBuilderWithInputStream() throws IOException {
        // Given
        var in = new ByteArrayInputStream(compressedData);

        // When & Then
        try (var decompressor = new XZDecompressorBuilder(in).build()) {
            assertThat(decompressor).isNotNull();
        }
    }

    @Test
    void testBuilderWithPath() throws IOException {
        // When & Then
        try (var decompressor = new XZDecompressorBuilder(compressedFile).build()) {
            assertThat(decompressor).isNotNull();
        }
    }

    @Test
    void testBuilderWithFile() throws IOException {
        // Given
        var file = compressedFile.toFile();

        // When & Then
        try (var decompressor = new XZDecompressorBuilder(file).build()) {
            assertThat(decompressor).isNotNull();
        }
    }

    @Test
    void testCompressorInputStreamBuilder() {
        // Given
        var in = new ByteArrayInputStream(compressedData);
        var builder = new XZDecompressorBuilder(in);

        // When
        XZDecompressorInputStreamBuilder<XZDecompressorBuilder> innerBuilder = builder.compressorInputStreamBuilder();

        // Then
        assertThat(innerBuilder).isNotNull();
        assertThat(innerBuilder.parentBuilder()).isSameAs(builder);
    }

    @Test
    void testBuildCompressorInputStream() throws IOException {
        // Given
        var in = new ByteArrayInputStream(compressedData);
        var builder = new XZDecompressorBuilder(in);

        builder.compressorInputStreamBuilder().setMemoryLimitInKb(10240);

        // When
        try (var xzIn = builder.buildCompressorInputStream()) {

            // Then
            assertThat(xzIn).isNotNull();

            var outputStream = new ByteArrayOutputStream();
            xzIn.transferTo(outputStream);
            assertThat(outputStream.toByteArray()).isEqualTo(TEST_DATA.getBytes(UTF_8));
        }
    }

    @Test
    void testGetThis() {
        // Given
        var in = new ByteArrayInputStream(compressedData);

        // When
        var builder = new XZDecompressorBuilder(in);

        // Then
        assertThat(builder.getThis()).isSameAs(builder);
    }

    @Test
    void testBuild() throws IOException {
        // Given
        var in = new ByteArrayInputStream(compressedData);

        // When
        try (var decompressor = new XZDecompressorBuilder(in)
                .compressorInputStreamBuilder()
                .setDecompressConcatenated(true)
                .parentBuilder()
                .build()) {

            // Then
            assertThat(decompressor).isNotNull();

            var tempFile = tempDir.resolve("buildTest.txt");
            decompressor.write(tempFile);
            assertThat(Files.readAllBytes(tempFile)).isEqualTo(TEST_DATA.getBytes(UTF_8));
        }
    }
}
