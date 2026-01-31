/*
 * Copyright 2024-2026 The Compress4J Project
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
package io.github.compress4j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.archivers.ArchiveFormat;
import io.github.compress4j.archivers.CompressionType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for {@link ArchiveBuilder} via the {@link Compress4J}
 * facade.
 *
 * @since 3.0
 */
class ArchiveBuilderIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateTarArchive() throws IOException {
        // Given: some test files
        Path file1 = tempDir.resolve("file1.txt");
        Files.writeString(file1, "Hello TAR!");

        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file2, "Second file");

        Path outputArchive = tempDir.resolve("test.tar");

        // When: creating a TAR archive
        try (var builder = Compress4J.create(outputArchive, ArchiveFormat.TAR)) {
            builder.add(file1).add(file2).build();
        }

        // Then: archive should exist and contain both files
        assertThat(outputArchive).exists();

        List<String> entries = Compress4J.list(outputArchive);
        assertThat(entries).containsExactlyInAnyOrder("file1.txt", "file2.txt");
    }

    @Test
    void testCreateZipArchive() throws IOException {
        // Given: some test files
        Path file1 = tempDir.resolve("test.txt");
        Files.writeString(file1, "Hello ZIP!");

        Path outputArchive = tempDir.resolve("test.zip");

        // When: creating a ZIP archive with custom entry name
        try (var builder = Compress4J.create(outputArchive, ArchiveFormat.ZIP)) {
            builder.add("docs/readme.txt", file1).build();
        }

        // Then: archive should contain entry with custom name
        assertThat(outputArchive).exists();

        List<String> entries = Compress4J.list(outputArchive);
        assertThat(entries).contains("docs/readme.txt");
    }

    @Test
    void testCreateTarGzArchive() throws IOException {
        // Given: some test content
        Path file1 = tempDir.resolve("data.txt");
        Files.writeString(file1, "Compressed content!");

        Path outputArchive = tempDir.resolve("test.tar.gz");

        // When: creating a compressed TAR archive
        try (var builder = Compress4J.create(outputArchive, ArchiveFormat.TAR)
                .withCompression(CompressionType.GZIP)) {
            builder.add(file1).build();
        }

        // Then: archive should be readable as tar.gz
        assertThat(outputArchive).exists();

        List<String> entries = Compress4J.list(outputArchive);
        assertThat(entries).contains("data.txt");
    }

    @Test
    void testCreateArchiveFromByteArray() throws IOException {
        // Given: byte content
        byte[] content = "Hello from bytes!".getBytes();
        Path outputArchive = tempDir.resolve("test.tar");

        // When: adding content from byte array
        try (var builder = Compress4J.create(outputArchive, ArchiveFormat.TAR)) {
            builder.add("message.txt", content).build();
        }

        // Then: archive should contain the entry
        assertThat(outputArchive).exists();

        List<String> entries = Compress4J.list(outputArchive);
        assertThat(entries).contains("message.txt");
    }

    @Test
    void testAddDirectory() throws IOException {
        // Given: a directory with files
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);

        Files.writeString(sourceDir.resolve("file1.txt"), "Content 1");
        Files.writeString(sourceDir.resolve("file2.txt"), "Content 2");

        Path subDir = Files.createDirectories(sourceDir.resolve("subdir"));
        Files.writeString(subDir.resolve("file3.txt"), "Content 3");

        Path outputArchive = tempDir.resolve("test.tar");

        // When: adding an entire directory recursively
        try (var builder = Compress4J.create(outputArchive, ArchiveFormat.TAR)) {
            builder.addDirectory(sourceDir).build();
        }

        // Then: all files should be in the archive
        assertThat(outputArchive).exists();

        List<String> entries = Compress4J.list(outputArchive);
        assertThat(entries).contains("source/file1.txt", "source/file2.txt", "source/subdir/file3.txt");
    }

    @Test
    void testWithFilter() throws IOException {
        // Given: a directory with mixed files
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);

        Files.writeString(sourceDir.resolve("keep.txt"), "Keep me");
        Files.writeString(sourceDir.resolve("skip.log"), "Skip me");
        Files.writeString(sourceDir.resolve("also-keep.txt"), "Also keep");

        Path outputArchive = tempDir.resolve("test.tar");

        // When: creating archive with file filter (only .txt files)
        try (var builder = Compress4J.create(outputArchive, ArchiveFormat.TAR)
                .withFilter((name, path) -> name.endsWith(".txt"))) {
            builder.addDirectory(sourceDir).build();
        }

        // Then: only .txt files should be included
        assertThat(outputArchive).exists();

        List<String> entries = Compress4J.list(outputArchive);
        assertThat(entries).contains("source/keep.txt", "source/also-keep.txt").doesNotContain("source/skip.log");
    }

    @Test
    void testRoundTrip() throws IOException {
        // Given: original content
        Path file1 = tempDir.resolve("original.txt");
        String originalContent = "Original content for round-trip test";
        Files.writeString(file1, originalContent);

        Path archivePath = tempDir.resolve("roundtrip.tar");
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        // When: creating archive and extracting it back
        try (var builder = Compress4J.create(archivePath, ArchiveFormat.TAR)) {
            builder.add(file1).build();
        }

        Compress4J.extractAll(archivePath, extractDir);

        // Then: extracted file should match original
        Path extractedFile = extractDir.resolve("original.txt");
        assertThat(extractedFile).exists();
        assertThat(Files.readString(extractedFile)).isEqualTo(originalContent);
    }
}
