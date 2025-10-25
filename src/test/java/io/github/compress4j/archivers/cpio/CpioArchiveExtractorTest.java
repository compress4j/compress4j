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
package io.github.compress4j.archivers.cpio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CpioArchiveExtractorTest {

    @TempDir
    Path tempDir;

    private byte[] sampleArchive;
    private byte[] directoryArchive;

    @BeforeEach
    void setUp() throws IOException {
        // Create a sample CPIO archive for testing extraction
        sampleArchive = createSampleArchive();
        directoryArchive = createDirectoryArchive();
    }

    @Test
    void testExtractCpioArchive() throws IOException {
        // given
        var extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        // when
        var archiveInput = new ByteArrayInputStream(sampleArchive);
        try (var archiveInputStream = new CpioArchiveInputStream(archiveInput);
                var extractor = new CpioArchiveExtractor(archiveInputStream)) {
            extractor.extract(extractDir);
        }

        // then
        assertThat(extractDir.resolve("file1.txt")).exists().hasContent("Content 1");
        assertThat(extractDir.resolve("file2.txt")).exists().hasContent("Content 2");
    }

    @Test
    void testExtractCpioArchiveWithDirectories() throws IOException {
        // given
        var extractDir = tempDir.resolve("extract-dirs");
        Files.createDirectories(extractDir);

        // when
        var archiveInput = new ByteArrayInputStream(directoryArchive);
        try (var extractor = CpioArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // then
        var extractedDir = extractDir.resolve("test_dir");
        var extractedFile = extractedDir.resolve("nested.txt");

        assertThat(extractedDir).exists().isDirectory();
        assertThat(extractedFile).exists().hasContent("Nested content");
    }

    @Test
    void testExtractFromFile() throws IOException {
        // given
        var archiveFile = tempDir.resolve("test.cpio");
        Files.write(archiveFile, sampleArchive);

        var extractDir = tempDir.resolve("extract-from-file");
        Files.createDirectories(extractDir);

        // when
        try (var extractor = CpioArchiveExtractor.builder(archiveFile).build()) {
            extractor.extract(extractDir);
        }

        // then
        assertThat(extractDir.resolve("file1.txt")).exists();
        assertThat(extractDir.resolve("file2.txt")).exists();
    }

    @Test
    void testExtractFromFileObject() throws IOException {
        // given
        var archiveFile = tempDir.resolve("test.cpio");
        Files.write(archiveFile, sampleArchive);

        var extractDir = tempDir.resolve("extract-from-file-obj");
        Files.createDirectories(extractDir);

        // when
        try (var extractor = CpioArchiveExtractor.builder(archiveFile.toFile()).build()) {
            extractor.extract(extractDir);
        }

        // then
        assertThat(extractDir.resolve("file1.txt")).exists();
        assertThat(extractDir.resolve("file2.txt")).exists();
    }

    @Test
    void testExtractorBuilderConfiguration() throws IOException {
        // given
        var extractDir = tempDir.resolve("extract-config");
        Files.createDirectories(extractDir);

        // when
        var archiveInput = new ByteArrayInputStream(sampleArchive);
        try (var extractor = CpioArchiveExtractor.builder(archiveInput)
                .cpioInputStream()
                .blockSize(1024)
                .encoding("UTF-8")
                .and()
                .build()) {
            extractor.extract(extractDir);
        }

        // then
        assertThat(extractDir.resolve("file1.txt")).exists();
        assertThat(extractDir.resolve("file2.txt")).exists();
    }

    @Test
    void testExtractEmptyArchive() throws IOException {
        // when
        var emptyArchiveOutput = new ByteArrayOutputStream();
        //noinspection EmptyTryBlock
        try (CpioArchiveCreator ignored =
                CpioArchiveCreator.builder(emptyArchiveOutput).build()) {
            // Don't add any files - archive will only contain TRAILER
        }
        byte[] emptyArchive = emptyArchiveOutput.toByteArray();

        var extractDir = tempDir.resolve("extract-empty");
        Files.createDirectories(extractDir);

        // when
        var archiveInput = new ByteArrayInputStream(emptyArchive);
        try (var extractor = CpioArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // then
        assertThat(extractDir).isEmptyDirectory();
    }

    @Test
    void testExtractWithOverwrite() throws IOException {
        var extractDir = tempDir.resolve("extract-overwrite");
        Files.createDirectories(extractDir);

        // Create existing file
        Path existingFile = extractDir.resolve("file1.txt");
        Files.write(existingFile, "Existing content".getBytes());

        // Extract archive (should overwrite)
        var archiveInput = new ByteArrayInputStream(sampleArchive);
        try (var extractor =
                CpioArchiveExtractor.builder(archiveInput).overwrite(true).build()) {
            extractor.extract(extractDir);
        }

        // Verify file was overwritten
        assertThat(Files.readString(existingFile)).isEqualTo("Content 1");
    }

    @Test
    void testExtractWithSpecialCharacterFiles() throws IOException {
        // given
        Path specialFile = tempDir.resolve("special-äöü.txt");
        Files.write(specialFile, "Special content".getBytes());

        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (CpioArchiveCreator creator = CpioArchiveCreator.builder(archiveOutput)
                .cpioOutputStream()
                .encoding("UTF-8")
                .and()
                .build()) {
            creator.addFile("special-äöü.txt", specialFile);
        }

        // when
        var extractDir = tempDir.resolve("extract-special");
        Files.createDirectories(extractDir);

        var archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (var extractor = CpioArchiveExtractor.builder(archiveInput)
                .cpioInputStream()
                .encoding("UTF-8")
                .and()
                .build()) {
            extractor.extract(extractDir);
        }

        // then
        assertThat(extractDir.resolve("special-äöü.txt")).exists().hasContent("Special content");
    }

    @Test
    void testExtractNonExistentFile() {
        // given
        var nonExistentFile = tempDir.resolve("does-not-exist.cpio");

        // when & then
        assertThatThrownBy(() -> CpioArchiveExtractor.builder(nonExistentFile)).isInstanceOf(IOException.class);
    }

    @Test
    void testExtractToNonExistentDirectory() throws IOException {
        // given
        var nonExistentDir = tempDir.resolve("does-not-exist/extract");

        var archiveInput = new ByteArrayInputStream(sampleArchive);
        try (var extractor = CpioArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(nonExistentDir);
        }

        // then
        assertThat(nonExistentDir)
                .exists()
                .isDirectory()
                .isDirectoryContaining(file -> file.endsWith("file1.txt"))
                .isDirectoryContaining(file -> file.endsWith("file2.txt"));
    }

    @Test
    void testExtractLargeFile() throws IOException {
        // given
        Path largeFile = tempDir.resolve("large-file.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("This is line ").append(i).append(" of the large file.\n");
        }
        Files.write(largeFile, content.toString().getBytes());

        // Create archive with large file
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (CpioArchiveCreator creator =
                CpioArchiveCreator.builder(archiveOutput).build()) {
            creator.addFile("large.txt", largeFile);
        }

        // when
        var extractDir = tempDir.resolve("extract-large");
        Files.createDirectories(extractDir);

        var archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (var extractor = CpioArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // then
        assertThat(extractDir.resolve("large.txt")).exists().hasContent(content.toString());
    }

    private byte[] createSampleArchive() throws IOException {
        Path testFile1 = tempDir.resolve("setup-test1.txt");
        Path testFile2 = tempDir.resolve("setup-test2.txt");
        Files.write(testFile1, "Content 1".getBytes());
        Files.write(testFile2, "Content 2".getBytes());

        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (CpioArchiveCreator creator =
                CpioArchiveCreator.builder(archiveOutput).build()) {
            creator.addFile("file1.txt", testFile1);
            creator.addFile("file2.txt", testFile2);
        }
        return archiveOutput.toByteArray();
    }

    private byte[] createDirectoryArchive() throws IOException {
        Path subDir = tempDir.resolve("setup-subdir");
        Files.createDirectories(subDir);
        Path nestedFile = subDir.resolve("nested.txt");
        Files.write(nestedFile, "Nested content".getBytes());

        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (CpioArchiveCreator creator =
                CpioArchiveCreator.builder(archiveOutput).build()) {
            creator.addDirectory("test_dir/", FileTime.fromMillis(System.currentTimeMillis()));
            creator.addFile("test_dir/nested.txt", nestedFile);
        }
        return archiveOutput.toByteArray();
    }
}
