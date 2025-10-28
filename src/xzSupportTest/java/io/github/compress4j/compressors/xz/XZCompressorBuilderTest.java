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

import io.github.compress4j.compressors.xz.XZCompressor.XZCompressorBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tukaani.xz.LZMA2Options;

/** Tests for the {@link XZCompressorBuilder} class. */
class XZCompressorBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void testConstructorWithPath() throws IOException {
        // Given
        var compressedFile = tempDir.resolve("test.xz");

        // When
        var builder = new XZCompressorBuilder(compressedFile);

        // Then
        assertThat(builder).isNotNull();
        assertThat(compressedFile).exists();

        try (var compressor = builder.build()) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void testConstructorWithPathFailsOnDirectory() {
        // When & Then
        assertThatThrownBy(() -> new XZCompressorBuilder(tempDir)).isInstanceOf(IOException.class);
    }

    @Test
    void testConstructorWithOutputStream() throws IOException {
        // Given
        var outputStream = new ByteArrayOutputStream();

        // When
        var builder = new XZCompressorBuilder(outputStream);

        // Then
        assertThat(builder).isNotNull();

        try (var compressor = builder.build()) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void testConstructorWithNullPath() {
        // Given
        Path path = null;

        // When & Then
        //noinspection ConstantValue
        assertThatThrownBy(() -> new XZCompressorBuilder(path)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testGetThis() throws IOException {
        // Given
        var outputStream = new ByteArrayOutputStream();
        var builder = new XZCompressorBuilder(outputStream);

        // When
        var self = builder.getThis();

        // Then
        assertThat(self).isSameAs(builder);
        try (var compressor = builder.build()) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void testCompressorOutputStreamBuilderGetter() throws IOException {
        // Given
        var outputStream = new ByteArrayOutputStream();
        var builder = new XZCompressorBuilder(outputStream);

        // When
        XZCompressor.XZCompressorOutputStreamBuilder<XZCompressorBuilder> osBuilder =
                builder.compressorOutputStreamBuilder();

        // Then
        assertThat(osBuilder).isNotNull();
        assertThat(osBuilder.parentBuilder()).isSameAs(builder);

        try (var compressor = builder.build()) {
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void testBuildCompressorOutputStream() throws IOException {
        // Given
        var outputStream = new ByteArrayOutputStream();
        var builder = new XZCompressorBuilder(outputStream);

        // When
        try (var stream = builder.buildCompressorOutputStream()) {
            // Then
            assertThat(stream).isNotNull();
        }
    }

    @Test
    void testBuild() throws IOException {
        // Given
        var outputStream = new ByteArrayOutputStream();
        var builder = new XZCompressorBuilder(outputStream);

        // When
        try (var compressor = builder.build()) {
            // Then
            assertThat(compressor).isNotNull();
        }
    }

    @Test
    void testFluentChaining() throws IOException {
        // Given
        var compressedFile = tempDir.resolve("test.xz");

        // When
        try (var compressor = XZCompressor.builder(compressedFile)
                .compressorOutputStreamBuilder()
                .preset(LZMA2Options.PRESET_MAX)
                .lzma2Options(new LZMA2Options(1))
                .parentBuilder()
                .getThis()
                .build()) {

            // Then
            assertThat(compressor).isNotNull();
        }

        // Then
        assertThat(Files.size(compressedFile)).isGreaterThan(0L);
    }
}
