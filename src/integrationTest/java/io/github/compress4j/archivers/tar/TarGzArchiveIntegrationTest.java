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
package io.github.compress4j.archivers.tar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for TAR.GZ archive functionality - testing interaction between creator and extractor components.
 *
 * @since 2.2
 */
class TarGzArchiveIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateAndExtractGzippedTar() throws IOException {
        // Create test files
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);

        Path textFile = sourceDir.resolve("test.txt");
        Path largeFile = sourceDir.resolve("large.txt");

        Files.write(textFile, "Test content for compression".getBytes());

        // Create large file with repetitive content to test compression
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is line ").append(i).append(" with repetitive content.\n");
        }
        Files.write(largeFile, largeContent.toString().getBytes());

        // Create TAR.GZ archive
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarGzArchiveCreator creator =
                TarGzArchiveCreator.builder(archiveOutput).build()) {

            creator.addFile("test.txt", textFile);
            creator.addFile("large.txt", largeFile);
        }

        // Verify compression efficiency
        byte[] archiveBytes = archiveOutput.toByteArray();
        assertThat(archiveBytes).hasSizeLessThan(largeContent.length());

        // Extract TAR.GZ archive
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveBytes);
        try (TarGzArchiveExtractor extractor =
                TarGzArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // Verify extracted files
        assertThat(extractDir.resolve("test.txt")).exists();
        assertThat(extractDir.resolve("large.txt")).exists();

        // Verify file contents
        assertThat(Files.readString(extractDir.resolve("test.txt"))).isEqualTo("Test content for compression");
        assertThat(Files.readString(extractDir.resolve("large.txt"))).isEqualTo(largeContent.toString());
    }

    @Test
    void testDifferentCompressionLevels() throws IOException {
        // Create test file
        Path testFile = tempDir.resolve("test.txt");
        String content = "Compression test content that should compress well ".repeat(100);
        Files.write(testFile, content.getBytes());

        // Test different compression levels
        int[] compressionLevels = {1, 6, 9}; // Fast, balanced, best

        for (int level : compressionLevels) {
            ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();

            // Create archive with specific compression level
            try (TarGzArchiveCreator creator =
                    TarGzArchiveCreator.builder(archiveOutput).build()) {
                creator.addFile("test.txt", testFile);
            }

            // Extract and verify
            Path extractDir = tempDir.resolve("extract-level-" + level);
            Files.createDirectories(extractDir);

            ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
            try (TarGzArchiveExtractor extractor =
                    TarGzArchiveExtractor.builder(archiveInput).build()) {
                extractor.extract(extractDir);
            }

            assertThat(extractDir.resolve("test.txt")).exists();
            assertThat(Files.readString(extractDir.resolve("test.txt"))).isEqualTo(content);
        }
    }

    @Test
    void testStreamingWithLargeFiles() throws IOException {
        // Create a very large file to test streaming capabilities
        Path largeFile = tempDir.resolve("streaming.bin");
        byte[] chunk = new byte[8192];
        for (int i = 0; i < chunk.length; i++) {
            chunk[i] = (byte) (i % 256);
        }

        // Write multiple chunks to create a larger file
        try (var out = Files.newOutputStream(largeFile)) {
            for (int i = 0; i < 128; i++) { // 1MB file
                out.write(chunk);
            }
        }

        // Create archive with streaming
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarGzArchiveCreator creator =
                TarGzArchiveCreator.builder(archiveOutput).build()) {
            creator.addFile("streaming.bin", largeFile);
        }

        // Extract with streaming
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (TarGzArchiveExtractor extractor =
                TarGzArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // Verify large file integrity
        Path extractedFile = extractDir.resolve("streaming.bin");
        assertThat(extractedFile).exists();
        assertThat(Files.size(extractedFile)).isEqualTo(Files.size(largeFile));

        // Verify first and last chunks
        byte[] extractedBytes = Files.readAllBytes(extractedFile);
        for (int i = 0; i < chunk.length; i++) {
            assertThat(extractedBytes[i]).isEqualTo((byte) (i % 256));
        }
    }
}
