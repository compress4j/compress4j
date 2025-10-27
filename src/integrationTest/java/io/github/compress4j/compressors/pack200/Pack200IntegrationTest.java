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
package io.github.compress4j.compressors.pack200;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.commons.compress.compressors.pack200.Pack200Strategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for Pack200 compressor and decompressor.
 *
 * <p>Note: Pack200 is specifically designed for JAR files only. These tests create minimal JAR files to test the
 * compression/decompression functionality.
 */
class Pack200IntegrationTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldCompressAndDecompressJarFile() throws IOException {
        // Given - Create a simple JAR file
        var sourceJar = tempDir.resolve("test.jar");
        createSimpleJar(sourceJar);

        var compressedFile = tempDir.resolve("test.pack");
        var decompressedJar = tempDir.resolve("decompressed.jar");

        // When - Compress the JAR
        try (var compressor = Pack200Compressor.builder(compressedFile).build()) {
            compressor.write(sourceJar);
        }

        // Then - Verify compression created a file
        assertThat(compressedFile).exists();
        assertThat(Files.size(compressedFile)).isGreaterThan(0);

        // When - Decompress the file
        try (var decompressor = Pack200Decompressor.builder(compressedFile).build()) {
            decompressor.write(decompressedJar);
        }

        // Then - Verify decompression created a JAR file
        assertThat(decompressedJar).exists();
        assertThat(Files.size(decompressedJar)).isGreaterThan(0);
    }

    @Test
    void shouldCompressWithCustomProperties() throws IOException {
        // Given - Create a simple JAR file
        var sourceJar = tempDir.resolve("test.jar");
        createSimpleJar(sourceJar);

        var compressedFile = tempDir.resolve("test.pack");

        // When - Compress with custom properties
        try (var compressor = Pack200Compressor.builder(compressedFile)
                .compressorOutputStreamBuilder()
                .mode(Pack200Strategy.IN_MEMORY)
                .parentBuilder()
                .build()) {
            compressor.write(sourceJar);
        }

        // Then - Verify compression succeeded
        assertThat(compressedFile).exists();
        assertThat(Files.size(compressedFile)).isGreaterThan(0);
    }

    @Test
    void shouldDecompressWithCustomProperties() throws IOException {
        // Given - Create and compress a JAR file
        var sourceJar = tempDir.resolve("test.jar");
        createSimpleJar(sourceJar);

        var compressedFile = tempDir.resolve("test.pack");
        try (var compressor = Pack200Compressor.builder(compressedFile).build()) {
            compressor.write(sourceJar);
        }

        var decompressedJar = tempDir.resolve("decompressed.jar");

        // When - Decompress with custom properties
        try (var decompressor = Pack200Decompressor.builder(compressedFile)
                .compressorInputStreamBuilder()
                .mode(Pack200Strategy.IN_MEMORY)
                .parentBuilder()
                .build()) {
            decompressor.write(decompressedJar);
        }

        // Then - Verify decompression succeeded
        assertThat(decompressedJar).exists();
        assertThat(Files.size(decompressedJar)).isGreaterThan(0);
    }

    @Test
    void shouldCompressWithOutputStream() throws IOException {
        // Given - Create a simple JAR file
        var sourceJar = tempDir.resolve("test.jar");
        createSimpleJar(sourceJar);

        var outputStream = new ByteArrayOutputStream();

        // When - Compress using OutputStream
        try (var compressor = Pack200Compressor.builder(outputStream).build()) {
            compressor.write(sourceJar);
        }

        // Then - Verify compression produced data
        assertThat(outputStream.size()).isGreaterThan(0);
    }

    /** Creates a minimal JAR file for testing purposes. Pack200 requires valid JAR structure with manifest. */
    private void createSimpleJar(Path jarPath) throws IOException {
        try (var fos = new FileOutputStream(jarPath.toFile());
                var jos = new JarOutputStream(fos)) {

            // Add a simple text file entry
            var entry = new JarEntry("test.txt");
            jos.putNextEntry(entry);
            jos.write("Test content for Pack200 compression".getBytes());
            jos.closeEntry();
        }
    }
}
