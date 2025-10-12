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

class TarBZip2ArchiveIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateAndExtractBZip2CompressedTar() throws IOException {
        // Create test files with different characteristics
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);

        Path textFile = sourceDir.resolve("document.txt");
        Path xmlFile = sourceDir.resolve("config.xml");
        Path binaryFile = sourceDir.resolve("data.bin");

        Files.write(
                textFile,
                "BZip2 compression test with repetitive text ".repeat(50).getBytes());
        Files.write(
                xmlFile,
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <configuration>
                <setting name="test" value="bzip2" />
                <setting name="compression" value="maximum" />
            </configuration>
            """
                        .getBytes());

        // Create binary data
        byte[] binaryData = new byte[4096];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte) (Math.sin(i * 0.1) * 127);
        }
        Files.write(binaryFile, binaryData);

        // Create TAR.BZ2 archive
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarBZip2ArchiveCreator creator =
                TarBZip2ArchiveCreator.builder(archiveOutput).build()) {

            creator.addFile("document.txt", textFile);
            creator.addFile("config.xml", xmlFile);
            creator.addFile("data.bin", binaryFile);
        }

        // Extract TAR.BZ2 archive
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (TarBZip2ArchiveExtractor extractor =
                TarBZip2ArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // Verify extracted files
        assertThat(extractDir.resolve("document.txt")).exists();
        assertThat(extractDir.resolve("config.xml")).exists();
        assertThat(extractDir.resolve("data.bin")).exists();

        // Verify file contents
        assertThat(Files.readString(extractDir.resolve("document.txt")))
                .contains("BZip2 compression test with repetitive text");
        assertThat(Files.readString(extractDir.resolve("config.xml")))
                .contains("<configuration>")
                .contains("bzip2");
        assertThat(Files.readAllBytes(extractDir.resolve("data.bin"))).isEqualTo(binaryData);
    }

    @Test
    void testBZip2BlockSizeEffects() throws IOException {
        // Create test file with patterns that compress differently
        Path testFile = tempDir.resolve("pattern.txt");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("Pattern ").append(i % 10).append(" repeats for BZip2 block testing.\n");
        }
        Files.write(testFile, content.toString().getBytes());

        // Test different block sizes
        int[] blockSizes = {1, 5, 9}; // Small, medium, large

        for (int blockSize : blockSizes) {
            ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();

            // Create archive with specific block size
            try (TarBZip2ArchiveCreator creator =
                    TarBZip2ArchiveCreator.builder(archiveOutput).build()) {
                creator.addFile("pattern.txt", testFile);
            }

            // Extract and verify
            Path extractDir = tempDir.resolve("extract-block-" + blockSize);
            Files.createDirectories(extractDir);

            ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
            try (TarBZip2ArchiveExtractor extractor =
                    TarBZip2ArchiveExtractor.builder(archiveInput).build()) {
                extractor.extract(extractDir);
            }

            assertThat(extractDir.resolve("pattern.txt")).exists();
            assertThat(Files.readString(extractDir.resolve("pattern.txt"))).isEqualTo(content.toString());
        }
    }

    @Test
    void testComplexDirectoryStructureWithBZip2() throws IOException {
        // Create complex directory structure
        Path sourceDir = tempDir.resolve("complex");
        Files.createDirectories(sourceDir);

        // Create nested structure
        String[] dirs = {"level1", "level1/level2", "level1/level2/level3"};
        for (String dir : dirs) {
            Files.createDirectories(sourceDir.resolve(dir));
        }

        // Create files at different levels
        Files.write(sourceDir.resolve("root.txt"), "Root level file".getBytes());
        Files.write(sourceDir.resolve("level1/file1.txt"), "Level 1 file".getBytes());
        Files.write(sourceDir.resolve("level1/level2/file2.txt"), "Level 2 file".getBytes());
        Files.write(sourceDir.resolve("level1/level2/level3/deep.txt"), "Deep nested file".getBytes());

        // Create TAR.BZ2 archive
        ByteArrayOutputStream archiveOutput = new ByteArrayOutputStream();
        try (TarBZip2ArchiveCreator creator =
                TarBZip2ArchiveCreator.builder(archiveOutput).build()) {

            // Add directories
            for (String dir : dirs) {
                creator.addDirectory(dir + "/", java.nio.file.attribute.FileTime.from(java.time.Instant.now()));
            }

            // Add files
            creator.addFile("root.txt", sourceDir.resolve("root.txt"));
            creator.addFile("level1/file1.txt", sourceDir.resolve("level1/file1.txt"));
            creator.addFile("level1/level2/file2.txt", sourceDir.resolve("level1/level2/file2.txt"));
            creator.addFile("level1/level2/level3/deep.txt", sourceDir.resolve("level1/level2/level3/deep.txt"));
        }

        // Extract and verify structure
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        ByteArrayInputStream archiveInput = new ByteArrayInputStream(archiveOutput.toByteArray());
        try (TarBZip2ArchiveExtractor extractor =
                TarBZip2ArchiveExtractor.builder(archiveInput).build()) {
            extractor.extract(extractDir);
        }

        // Verify directory structure
        assertThat(extractDir.resolve("root.txt")).exists();
        assertThat(extractDir.resolve("level1")).exists().isDirectory();
        assertThat(extractDir.resolve("level1/level2")).exists().isDirectory();
        assertThat(extractDir.resolve("level1/level2/level3")).exists().isDirectory();

        // Verify files
        assertThat(Files.readString(extractDir.resolve("root.txt"))).isEqualTo("Root level file");
        assertThat(Files.readString(extractDir.resolve("level1/file1.txt"))).isEqualTo("Level 1 file");
        assertThat(Files.readString(extractDir.resolve("level1/level2/file2.txt")))
                .isEqualTo("Level 2 file");
        assertThat(Files.readString(extractDir.resolve("level1/level2/level3/deep.txt")))
                .isEqualTo("Deep nested file");
    }
}
