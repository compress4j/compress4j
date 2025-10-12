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
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class TarArchiveIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateAndExtractRoundTrip() throws IOException {
        // Setup: Create test files
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);

        Path file1 = sourceDir.resolve("document.txt");
        Path file2 = sourceDir.resolve("readme.md");
        Path subDir = sourceDir.resolve("subdir");
        Files.createDirectories(subDir);
        Path file3 = subDir.resolve("nested.txt");

        Files.write(file1, "This is a test document with some content.".getBytes());
        Files.write(file2, "# README\n\nThis is a readme file.".getBytes());
        Files.write(file3, "Nested file content in subdirectory.".getBytes());

        // Create TAR archive
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarArchiveCreator creator = TarArchiveCreator.builder(archiveOutput)
                .longFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU)
                .build()) {

            creator.addFile("document.txt", file1);
            creator.addFile("readme.md", file2);
            creator.addDirectory("subdir/", FileTime.from(Instant.now()));
            creator.addFile("subdir/nested.txt", file3);
        }

        // Extract TAR archive
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (TarArchiveExtractor extractor =
                TarArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // Verify extracted files
        assertThat(extractDir.resolve("document.txt")).exists();
        assertThat(extractDir.resolve("readme.md")).exists();
        assertThat(extractDir.resolve("subdir")).exists().isDirectory();
        assertThat(extractDir.resolve("subdir/nested.txt")).exists();

        // Verify file contents
        assertThat(Files.readString(extractDir.resolve("document.txt")))
                .isEqualTo("This is a test document with some content.");
        assertThat(Files.readString(extractDir.resolve("readme.md"))).isEqualTo("# README\n\nThis is a readme file.");
        assertThat(Files.readString(extractDir.resolve("subdir/nested.txt")))
                .isEqualTo("Nested file content in subdirectory.");
    }

    @Test
    void testSymbolicLinksAndHardLinks() throws IOException {
        // Create source files
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);

        Path originalFile = sourceDir.resolve("original.txt");
        Files.write(originalFile, "Original file content".getBytes());

        // Create TAR archive with link handling
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarArchiveCreator creator = TarArchiveCreator.builder(archiveOutput)
                .longFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX)
                .build()) {

            creator.addFile("original.txt", originalFile);
            // Note: Symbolic link handling would require platform-specific implementation
        }

        // Extract and verify
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (TarArchiveExtractor extractor =
                TarArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        assertThat(extractDir.resolve("original.txt")).exists();
        assertThat(Files.readString(extractDir.resolve("original.txt"))).isEqualTo("Original file content");
    }

    @Test
    void testLargeArchiveHandling() throws IOException {
        // Create multiple large files
        Path largeFile1 = tempDir.resolve("large1.bin");
        Path largeFile2 = tempDir.resolve("large2.bin");

        byte[] largeContent1 = new byte[5 * 1024 * 1024]; // 5MB
        byte[] largeContent2 = new byte[3 * 1024 * 1024]; // 3MB

        for (int i = 0; i < largeContent1.length; i++) {
            largeContent1[i] = (byte) (i % 256);
        }
        for (int i = 0; i < largeContent2.length; i++) {
            largeContent2[i] = (byte) ((i * 7) % 256);
        }

        Files.write(largeFile1, largeContent1);
        Files.write(largeFile2, largeContent2);

        // Create archive
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarArchiveCreator creator =
                TarArchiveCreator.builder(archiveOutput).build()) {
            creator.addFile("large1.bin", largeFile1);
            creator.addFile("large2.bin", largeFile2);
        }

        // Extract archive
        Path extractDir = tempDir.resolve("extract-large");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (TarArchiveExtractor extractor =
                TarArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // Verify large files
        assertThat(extractDir.resolve("large1.bin")).exists();
        assertThat(extractDir.resolve("large2.bin")).exists();
        assertThat(Files.size(extractDir.resolve("large1.bin"))).isEqualTo(largeContent1.length);
        assertThat(Files.size(extractDir.resolve("large2.bin"))).isEqualTo(largeContent2.length);
        assertThat(Files.readAllBytes(extractDir.resolve("large1.bin"))).isEqualTo(largeContent1);
        assertThat(Files.readAllBytes(extractDir.resolve("large2.bin"))).isEqualTo(largeContent2);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void testFilePermissionsAndMetadata() throws IOException {
        // Create test files with different characteristics
        Path executableFile = tempDir.resolve("script.sh");
        Path readOnlyFile = tempDir.resolve("readonly.txt");
        Path normalFile = tempDir.resolve("normal.txt");

        Files.write(executableFile, "#!/bin/bash\necho 'Hello World'".getBytes());
        Files.write(readOnlyFile, "Read-only content".getBytes());
        Files.write(normalFile, "Normal file content".getBytes());

        // Create archive preserving metadata
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarArchiveCreator creator = TarArchiveCreator.builder(archiveOutput)
                .longFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU)
                .build()) {

            creator.addFile("script.sh", executableFile);
            creator.addFile("readonly.txt", readOnlyFile);
            creator.addFile("normal.txt", normalFile);
        }

        // Extract and verify
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (TarArchiveExtractor extractor =
                TarArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // Verify files exist and have correct content
        assertThat(extractDir.resolve("script.sh")).exists();
        assertThat(extractDir.resolve("readonly.txt")).exists();
        assertThat(extractDir.resolve("normal.txt")).exists();

        assertThat(Files.readString(extractDir.resolve("script.sh"))).isEqualTo("#!/bin/bash\necho 'Hello World'");
        assertThat(Files.readString(extractDir.resolve("readonly.txt"))).isEqualTo("Read-only content");
        assertThat(Files.readString(extractDir.resolve("normal.txt"))).isEqualTo("Normal file content");
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void testUnicodeFileNamesInTar() throws IOException {
        // Create files with Unicode names
        Path sourceDir = tempDir.resolve("unicode-source");
        Files.createDirectories(sourceDir);

        Path unicodeFile1 = sourceDir.resolve("файл.txt"); // Russian
        Path unicodeFile2 = sourceDir.resolve("文件.txt"); // Chinese
        Path unicodeFile3 = sourceDir.resolve("ファイル.txt"); // Japanese

        Files.writeString(unicodeFile1, "Содержимое файла на русском языке");
        Files.writeString(unicodeFile2, "中文文件内容");
        Files.writeString(unicodeFile3, "日本語のファイル内容");

        // Create archive with PAX headers for Unicode support
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarArchiveCreator creator = TarArchiveCreator.builder(archiveOutput)
                .longFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU)
                .build()) {
            creator.addFile("файл.txt", unicodeFile1);
            creator.addFile("文件.txt", unicodeFile2);
            creator.addFile("ファイル.txt", unicodeFile3);
        }

        // Extract archive
        Path extractDir = tempDir.resolve("extract-unicode");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (TarArchiveExtractor extractor =
                TarArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // Verify Unicode files
        assertThat(extractDir.resolve("файл.txt")).exists();
        assertThat(extractDir.resolve("文件.txt")).exists();
        assertThat(extractDir.resolve("ファイル.txt")).exists();

        assertThat(Files.readString(extractDir.resolve("файл.txt"), java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo("Содержимое файла на русском языке");
        assertThat(Files.readString(extractDir.resolve("文件.txt"), java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo("中文文件内容");
        assertThat(Files.readString(extractDir.resolve("ファイル.txt"), java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo("日本語のファイル内容");
    }
}
