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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Additional tests for AR archive functionality */
class ArArchiveAdvancedTest {

    @Test
    void testCreateArArchiveWithMultipleFiles(@TempDir Path tempDir) throws IOException {
        // Create an AR archive with multiple files
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ArArchiveCreator creator = ArArchiveCreator.builder(baos).build()) {
            // Add multiple files
            creator.addFile("file1.txt", "Content of file 1".getBytes(StandardCharsets.UTF_8));
            creator.addFile("file2.txt", "Content of file 2".getBytes(StandardCharsets.UTF_8));
            creator.addFile("file3.txt", "Content of file 3".getBytes(StandardCharsets.UTF_8));
        }

        // Extract and verify all files
        byte[] archiveBytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(archiveBytes);

        try (ArArchiveExtractor extractor = ArArchiveExtractor.builder(bais).build()) {
            extractor.extract(tempDir);
        }

        // Verify all files were extracted correctly
        assertThat(tempDir.resolve("file1.txt")).exists();
        assertThat(tempDir.resolve("file2.txt")).exists();
        assertThat(tempDir.resolve("file3.txt")).exists();

        assertThat(Files.readString(tempDir.resolve("file1.txt"))).isEqualTo("Content of file 1");
        assertThat(Files.readString(tempDir.resolve("file2.txt"))).isEqualTo("Content of file 2");
        assertThat(Files.readString(tempDir.resolve("file3.txt"))).isEqualTo("Content of file 3");
    }

    @Test
    void testCreateArArchiveWithEmptyFile(@TempDir Path tempDir) throws IOException {
        // Create an AR archive with an empty file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ArArchiveCreator creator = ArArchiveCreator.builder(baos).build()) {
            creator.addFile("empty.txt", new byte[0]);
        }

        // Extract and verify
        byte[] archiveBytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(archiveBytes);

        try (ArArchiveExtractor extractor = ArArchiveExtractor.builder(bais).build()) {
            extractor.extract(tempDir);
        }

        Path emptyFile = tempDir.resolve("empty.txt");
        assertThat(emptyFile).exists();
        assertThat(Files.size(emptyFile)).isZero();
    }

    @Test
    void testCreateArArchiveWithBinaryFile(@TempDir Path tempDir) throws IOException {
        // Create an AR archive with binary content
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Create some binary data
        byte[] binaryData = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryData[i] = (byte) i;
        }

        try (ArArchiveCreator creator = ArArchiveCreator.builder(baos).build()) {
            creator.addFile("binary.dat", binaryData);
        }

        // Extract and verify
        byte[] archiveBytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(archiveBytes);

        try (ArArchiveExtractor extractor = ArArchiveExtractor.builder(bais).build()) {
            extractor.extract(tempDir);
        }

        Path binaryFile = tempDir.resolve("binary.dat");
        assertThat(binaryFile).exists();

        byte[] extractedData = Files.readAllBytes(binaryFile);
        assertThat(extractedData).isEqualTo(binaryData);
    }

    @Test
    void testCreateArArchiveFromPath(@TempDir Path tempDir) throws IOException {
        // Create a temporary file to add to the archive
        Path sourceFile = tempDir.resolve("source.txt");
        String content = "Content from file path";
        Files.writeString(sourceFile, content);

        // Create AR archive using file path
        Path archivePath = tempDir.resolve("test.ar");

        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            creator.addFile(sourceFile);
        }

        // Extract to a different directory
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath).build()) {
            extractor.extract(extractDir);
        }

        // Verify extraction
        Path extractedFile = extractDir.resolve("source.txt");
        assertThat(extractedFile).exists();
        assertThat(Files.readString(extractedFile)).isEqualTo(content);
    }
}
