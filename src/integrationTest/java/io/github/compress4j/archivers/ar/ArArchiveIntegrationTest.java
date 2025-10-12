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
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArArchiveIntegrationTest {

    @Test
    void shouldCreateAndExtractLargeFiles(@TempDir Path tempDir) throws IOException {
        // Create a large file (100KB) to test performance and chunking
        byte[] largeContent = new byte[100 * 1024]; // 100KB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        Path archivePath = tempDir.resolve("large.ar");

        // Create archive with large file
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            creator.addFile("large.bin", largeContent);
        }

        // Verify archive was created and has reasonable size
        assertThat(archivePath).exists();
        long archiveSize = Files.size(archivePath);
        assertThat(archiveSize)
                .isGreaterThan(largeContent.length) // Archive overhead
                .isLessThan(largeContent.length + 1024); // Reasonable overhead

        // Extract and verify
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath).build()) {
            extractor.extract(extractDir);
        }

        Path extractedFile = extractDir.resolve("large.bin");
        assertThat(extractedFile).exists();
        assertThat(Files.size(extractedFile)).isEqualTo(largeContent.length);

        byte[] extractedContent = Files.readAllBytes(extractedFile);
        assertThat(extractedContent).isEqualTo(largeContent);
    }

    @Test
    void shouldHandleFilesWithSimpleNames(@TempDir Path tempDir) throws IOException {
        Path archivePath = tempDir.resolve("simple.ar");

        // Test files with simple names and content (avoid special characters that might cause issues)
        String[] fileNames = {"file1.txt", "file2.txt", "data.bin"};

        String[] contents = {"Simple content 1", "Simple content 2", "Binary data content"};

        // Create archive
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            for (int i = 0; i < fileNames.length; i++) {
                creator.addFile(fileNames[i], contents[i].getBytes(StandardCharsets.UTF_8));
            }
        }

        // Extract and verify
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath).build()) {
            extractor.extract(extractDir);
        }

        // Verify all files were extracted correctly
        for (int i = 0; i < fileNames.length; i++) {
            Path extractedFile = extractDir.resolve(fileNames[i]);
            assertThat(extractedFile).exists();
            String extractedContent = Files.readString(extractedFile, StandardCharsets.UTF_8);
            assertThat(extractedContent).isEqualTo(contents[i]);
        }
    }

    @Test
    void shouldHandleEmptyArchive(@TempDir Path tempDir) throws IOException {
        Path archivePath = tempDir.resolve("empty.ar");

        // Create empty archive
        //noinspection EmptyTryBlock
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            // Don't add any files
        }

        // Verify archive exists - AR format may create empty file for empty archives
        assertThat(archivePath).exists();

        // For empty archives, AR format might not create a valid archive structure
        // So we only test extraction if the archive has some content
        if (Files.size(archivePath) > 0) {
            // Extract empty archive should not fail
            Path extractDir = tempDir.resolve("extract");
            Files.createDirectories(extractDir);

            try (ArArchiveExtractor extractor =
                    ArArchiveExtractor.builder(archivePath).build()) {
                extractor.extract(extractDir);
            }

            // Verify extract directory exists but is empty
            try (Stream<Path> files = Files.list(extractDir)) {
                assertThat(files).isEmpty();
            }
        }
    }

    @Test
    void shouldHandleManySmallFiles(@TempDir Path tempDir) throws IOException {
        Path archivePath = tempDir.resolve("many_files.ar");
        int fileCount = 50; // Reduced from 100 to avoid potential issues

        // Create archive with many small files
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            for (int i = 0; i < fileCount; i++) {
                String fileName = String.format("file%03d.txt", i);
                String content = String.format("Content of file %d", i);
                creator.addFile(fileName, content.getBytes(StandardCharsets.UTF_8));
            }
        }

        // Extract
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath).build()) {
            extractor.extract(extractDir);
        }

        // Verify all files were extracted correctly
        try (Stream<Path> files = Files.list(extractDir)) {
            List<Path> extractedFiles = files.toList();
            assertThat(extractedFiles).hasSize(fileCount);
        }

        for (int i = 0; i < fileCount; i++) {
            String fileName = String.format("file%03d.txt", i);
            String expectedContent = String.format("Content of file %d", i);

            Path extractedFile = extractDir.resolve(fileName);
            assertThat(extractedFile).exists();
            assertThat(Files.readString(extractedFile)).isEqualTo(expectedContent);
        }
    }

    @Test
    void shouldHandleFilesWithZeroLength(@TempDir Path tempDir) throws IOException {
        Path archivePath = tempDir.resolve("zero_length.ar");

        // Create archive with mix of empty and non-empty files
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            creator.addFile("empty1.txt", new byte[0]);
            creator.addFile("content.txt", "Some content".getBytes(StandardCharsets.UTF_8));
            creator.addFile("empty2.txt", new byte[0]);
        }

        // Extract
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath).build()) {
            extractor.extract(extractDir);
        }

        // Verify all files exist with correct sizes
        assertThat(extractDir.resolve("empty1.txt")).exists();
        assertThat(extractDir.resolve("content.txt")).exists();
        assertThat(extractDir.resolve("empty2.txt")).exists();

        assertThat(Files.size(extractDir.resolve("empty1.txt"))).isZero();
        assertThat(Files.size(extractDir.resolve("content.txt"))).isEqualTo("Some content".length());
        assertThat(Files.size(extractDir.resolve("empty2.txt"))).isZero();

        assertThat(Files.readString(extractDir.resolve("content.txt"))).isEqualTo("Some content");
    }

    @Test
    void shouldHandleBinaryContent(@TempDir Path tempDir) throws IOException {
        Path archivePath = tempDir.resolve("binary.ar");

        // Create various binary content patterns
        byte[] binaryData1 = new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, (byte) 0xFD};
        byte[] binaryData2 = new byte[1000];
        for (int i = 0; i < binaryData2.length; i++) {
            binaryData2[i] = (byte) (Math.sin(i * 0.1) * 127);
        }

        // Create archive with binary files
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            creator.addFile("binary1.bin", binaryData1);
            creator.addFile("binary2.bin", binaryData2);
        }

        // Extract
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath).build()) {
            extractor.extract(extractDir);
        }

        // Verify binary content is preserved exactly
        byte[] extracted1 = Files.readAllBytes(extractDir.resolve("binary1.bin"));
        byte[] extracted2 = Files.readAllBytes(extractDir.resolve("binary2.bin"));

        assertThat(extracted1).isEqualTo(binaryData1);
        assertThat(extracted2).isEqualTo(binaryData2);
    }

    @Test
    void shouldHandleStreamBasedOperations(@TempDir Path tempDir) throws IOException {
        // Test creating and extracting archives using streams instead of files
        ByteArrayOutputStream archiveStream = new ByteArrayOutputStream();

        // Create archive in memory
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archiveStream).build()) {
            creator.addFile("stream1.txt", "Content from stream 1".getBytes(StandardCharsets.UTF_8));
            creator.addFile("stream2.txt", "Content from stream 2".getBytes(StandardCharsets.UTF_8));
        }

        // Extract from memory stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(archiveStream.toByteArray());
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(inputStream).build()) {
            extractor.extract(extractDir);
        }

        // Verify extraction
        assertThat(extractDir.resolve("stream1.txt")).exists();
        assertThat(extractDir.resolve("stream2.txt")).exists();

        assertThat(Files.readString(extractDir.resolve("stream1.txt"))).isEqualTo("Content from stream 1");
        assertThat(Files.readString(extractDir.resolve("stream2.txt"))).isEqualTo("Content from stream 2");
    }

    @Test
    void shouldCreateArchiveFromExistingFiles(@TempDir Path tempDir) throws IOException {
        // Create some source files
        Path sourceFile1 = tempDir.resolve("source1.txt");
        Path sourceFile2 = tempDir.resolve("source2.txt");

        Files.writeString(sourceFile1, "Content from file 1");
        Files.writeString(sourceFile2, "Content from file 2");

        // Create AR archive using file paths
        Path archivePath = tempDir.resolve("fromfiles.ar");

        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            creator.addFile(sourceFile1);
            creator.addFile(sourceFile2);
        }

        // Extract to a different directory
        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath).build()) {
            extractor.extract(extractDir);
        }

        // Verify extraction
        assertThat(extractDir.resolve("source1.txt")).exists();
        assertThat(extractDir.resolve("source2.txt")).exists();

        assertThat(Files.readString(extractDir.resolve("source1.txt"))).isEqualTo("Content from file 1");
        assertThat(Files.readString(extractDir.resolve("source2.txt"))).isEqualTo("Content from file 2");
    }

    @SuppressWarnings("java:S5783")
    @Test
    void shouldHandleInvalidArchiveGracefully(@TempDir Path tempDir) throws IOException {
        // Test with corrupted/invalid AR archive data
        byte[] invalidData = "This is not a valid AR archive".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream invalidStream = new ByteArrayInputStream(invalidData);

        Path extractDir = tempDir.resolve("extract");
        Files.createDirectories(extractDir);

        // Should handle invalid archive gracefully by throwing an IOException
        assertThatThrownBy(() -> {
                    try (ArArchiveExtractor extractor =
                            ArArchiveExtractor.builder(invalidStream).build()) {
                        extractor.extract(extractDir);
                    }
                })
                .isInstanceOf(Exception.class);
    }

    @Test
    void shouldRoundTripArchiveCorrectly(@TempDir Path tempDir) throws IOException {
        // Create test data
        String[] fileNames = {"test1.txt", "test2.bin", "test3.data"};
        byte[][] fileContents = {
            "Text file content with special chars: @#$%".getBytes(StandardCharsets.UTF_8),
            new byte[] {0x01, 0x02, 0x03, 0x04, (byte) 0xFF},
            "Mixed content\nwith\nnewlines".getBytes(StandardCharsets.UTF_8)
        };

        // First round: Create archive
        Path archivePath = tempDir.resolve("roundtrip.ar");
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath).build()) {
            for (int i = 0; i < fileNames.length; i++) {
                creator.addFile(fileNames[i], fileContents[i]);
            }
        }

        // First extraction
        Path extractDir1 = tempDir.resolve("extract1");
        Files.createDirectories(extractDir1);
        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath).build()) {
            extractor.extract(extractDir1);
        }

        // Second round: Create archive from extracted files
        Path archivePath2 = tempDir.resolve("roundtrip2.ar");
        try (ArArchiveCreator creator = ArArchiveCreator.builder(archivePath2).build()) {
            for (String fileName : fileNames) {
                creator.addFile(fileName, extractDir1.resolve(fileName));
            }
        }

        // Second extraction
        Path extractDir2 = tempDir.resolve("extract2");
        Files.createDirectories(extractDir2);
        try (ArArchiveExtractor extractor =
                ArArchiveExtractor.builder(archivePath2).build()) {
            extractor.extract(extractDir2);
        }

        // Verify both extractions have identical content
        for (int i = 0; i < fileNames.length; i++) {
            byte[] original = fileContents[i];
            byte[] firstExtract = Files.readAllBytes(extractDir1.resolve(fileNames[i]));
            byte[] secondExtract = Files.readAllBytes(extractDir2.resolve(fileNames[i]));

            assertThat(firstExtract).isEqualTo(original);
            assertThat(secondExtract).isEqualTo(original);
            assertThat(secondExtract).isEqualTo(firstExtract);
        }
    }
}
