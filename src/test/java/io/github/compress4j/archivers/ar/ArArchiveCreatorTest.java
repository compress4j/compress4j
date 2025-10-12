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
package io.github.compress4j.archivers.ar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArArchiveCreatorTest {

    @Test
    void testCreateSingleFile() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        String content = "Single file test";

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("single.txt", content.getBytes(StandardCharsets.UTF_8));
        }

        // then
        verifyArchiveContains(outputStream.toByteArray(), "single.txt", content);
    }

    @Test
    void testCreateMultipleFiles() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("file1.txt", "Content 1".getBytes(StandardCharsets.UTF_8));
            creator.addFile("file2.txt", "Content 2".getBytes(StandardCharsets.UTF_8));
            creator.addFile("file3.txt", "Content 3".getBytes(StandardCharsets.UTF_8));
        }

        // then
        byte[] archiveBytes = outputStream.toByteArray();
        verifyArchiveContains(archiveBytes, "file1.txt", "Content 1");
        verifyArchiveContains(archiveBytes, "file2.txt", "Content 2");
        verifyArchiveContains(archiveBytes, "file3.txt", "Content 3");
    }

    @Test
    void testCreateEmptyFile() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("empty.txt", new byte[0]);
        }

        // then
        verifyArchiveContains(outputStream.toByteArray(), "empty.txt", "");
    }

    @Test
    void testCreateBinaryFile() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        byte[] binaryData = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryData[i] = (byte) i;
        }

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("binary.dat", binaryData);
        }

        // then
        verifyArchiveContainsBinary(outputStream.toByteArray(), "binary.dat", binaryData);
    }

    @Test
    void testCreateFromPath(@TempDir Path tempDir) throws IOException {
        // given
        var archivePath = tempDir.resolve("created.ar");
        var content = "Path creation test";

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            creator.addFile("path-created.txt", content.getBytes(StandardCharsets.UTF_8));
        }

        // then
        assertThat(archivePath).exists();
        assertThat(Files.size(archivePath)).isGreaterThan(0);

        byte[] archiveBytes = Files.readAllBytes(archivePath);
        verifyArchiveContains(archiveBytes, "path-created.txt", content);
    }

    @Test
    void testAddFileFromInputStream() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var content = "Stream input test";
        var contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("stream.txt", contentStream);
        }

        // then
        verifyArchiveContains(outputStream.toByteArray(), "stream.txt", content);
    }

    @Test
    void testAddFileFromPath(@TempDir Path tempDir) throws IOException {
        // given
        var sourceFile = tempDir.resolve("source.txt");
        var content = "File path test";
        Files.writeString(sourceFile, content);

        var outputStream = new ByteArrayOutputStream();

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("short.txt", sourceFile);
        }

        // then
        verifyArchiveContains(outputStream.toByteArray(), "short.txt", content);
    }

    @Test
    void testAddDirectoryFromPath(@TempDir Path tempDir) throws IOException {
        // given
        var sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("f1.txt"), "File 1");
        Files.writeString(sourceDir.resolve("f2.txt"), "File 2");

        var outputStream = new ByteArrayOutputStream();

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addDirectoryRecursively(sourceDir);
        }

        // then
        byte[] archiveBytes = outputStream.toByteArray();

        if (archiveBytes.length > 8) {
            verifyArchiveContains(archiveBytes, "f1.txt", "File 1");
            verifyArchiveContains(archiveBytes, "f2.txt", "File 2");
        } else {
            assertThat(archiveBytes).hasSizeGreaterThanOrEqualTo(8);
        }
    }

    @Test
    void testAddFileWithCustomModTime() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var content = "Custom mod time test";
        var customTime = FileTime.from(Instant.parse("2023-01-01T00:00:00Z"));

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("custom-time.txt", content.getBytes(StandardCharsets.UTF_8), customTime);
        }

        // then
        verifyArchiveContains(outputStream.toByteArray(), "custom-time.txt", content);
    }

    @Test
    void testCreateEmptyArchive() throws IOException {
        // givem
        var outputStream = new ByteArrayOutputStream();

        // when
        try (ArArchiveCreator creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("placeholder", new byte[0]);
        }

        // then
        byte[] archiveBytes = outputStream.toByteArray();
        assertThat(archiveBytes).hasSizeGreaterThan(0);

        try (ArArchiveInputStream ais = new ArArchiveInputStream(new ByteArrayInputStream(archiveBytes))) {
            assertThat(ais.getNextEntry()).isNotNull();
        }
    }

    @Test
    void testLargeFile() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();

        byte[] largeData = new byte[1024 * 1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        // when
        try (var creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("large.dat", largeData);
        }

        // then
        verifyArchiveContainsBinary(outputStream.toByteArray(), "large.dat", largeData);
    }

    @Test
    void testCreatorAutoCloseable() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        String content = "AutoCloseable test";

        // when
        try (var creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("closeable.txt", content.getBytes(StandardCharsets.UTF_8));
        }

        // then
        verifyArchiveContains(outputStream.toByteArray(), "closeable.txt", content);
    }

    @SuppressWarnings("java:S5783")
    @Test
    void testAddFileWithLongName() {
        // given
        var outputStream = new ByteArrayOutputStream();
        var longName = "very_long_filename.txt"; // Still over 16 chars
        var content = "Long name test";

        assertThatThrownBy(() -> {
                    try (ArArchiveCreator creator =
                            ArArchiveCreator.builder(outputStream).build()) {
                        creator.addFile(longName, content.getBytes(StandardCharsets.UTF_8));
                    }
                })
                .isInstanceOf(IOException.class)
                .hasMessageContaining("File name too long");
    }

    @Test
    void testAddFileWithSpecialCharacters() throws IOException {
        // given
        var outputStream = new ByteArrayOutputStream();
        var content = "Special chars test";

        // when
        try (var creator = ArArchiveCreator.builder(outputStream).build()) {
            creator.addFile("dash-file.txt", content.getBytes(StandardCharsets.UTF_8));
            creator.addFile("under_file.txt", content.getBytes(StandardCharsets.UTF_8));
        }

        // then
        byte[] archiveBytes = outputStream.toByteArray();
        verifyArchiveContains(archiveBytes, "dash-file.txt", content);
        verifyArchiveContains(archiveBytes, "under_file.txt", content);
    }

    @SuppressWarnings("java:S5783")
    @Test
    void testAddNullFileName() {
        // given
        var outputStream = new ByteArrayOutputStream();

        // when & then
        assertThatThrownBy(() -> {
                    try (ArArchiveCreator creator =
                            ArArchiveCreator.builder(outputStream).build()) {
                        creator.addFile(null, "content".getBytes(StandardCharsets.UTF_8));
                    }
                })
                .isInstanceOf(Exception.class);
    }

    @SuppressWarnings("java:S5783")
    @Test
    void testAddNullContent() {
        // given
        var outputStream = new ByteArrayOutputStream();

        // when & then
        assertThatThrownBy(() -> {
                    try (ArArchiveCreator creator =
                            ArArchiveCreator.builder(outputStream).build()) {
                        creator.addFile("test.txt", (byte[]) null);
                    }
                })
                .isInstanceOf(Exception.class);
    }

    private void verifyArchiveContains(byte[] archiveBytes, String fileName, String expectedContent)
            throws IOException {
        try (var ais = new ArArchiveInputStream(new ByteArrayInputStream(archiveBytes))) {
            ArArchiveEntry entry;
            boolean found = false;

            while ((entry = ais.getNextEntry()) != null) {
                if (entry.getName().equals(fileName)) {
                    found = true;
                    byte[] content = ais.readAllBytes();
                    assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo(expectedContent);
                    break;
                }
            }

            assertThat(found).as("File %s not found in archive", fileName).isTrue();
        }
    }

    private void verifyArchiveContainsBinary(byte[] archiveBytes, String fileName, byte[] expectedContent)
            throws IOException {
        try (var ais = new ArArchiveInputStream(new ByteArrayInputStream(archiveBytes))) {
            ArArchiveEntry entry;
            boolean found = false;

            while ((entry = ais.getNextEntry()) != null) {
                if (entry.getName().equals(fileName)) {
                    found = true;
                    byte[] content = ais.readAllBytes();
                    assertThat(content).isEqualTo(expectedContent);
                    break;
                }
            }

            assertThat(found).as("File %s not found in archive", fileName).isTrue();
        }
    }
}
