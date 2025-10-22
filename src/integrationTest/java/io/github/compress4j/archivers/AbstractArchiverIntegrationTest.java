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
package io.github.compress4j.archivers;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public abstract class AbstractArchiverIntegrationTest {

    @TempDir
    protected Path tempDir;

    protected abstract ArchiveCreator<?> archiveCreatorBuilder(Path archivePath) throws IOException;

    protected abstract ArchiveExtractor<?> archiveExtractorBuilder(Path archivePath) throws IOException;

    protected abstract String getExtension();

    protected Path getArchive() {
        try {
            String archivePathStr = "/archives/archive" + getExtension();
            URL resource = getClass().getResource(archivePathStr);
            Assertions.assertThat(resource)
                    .as("Archive file not found: " + archivePathStr + " in resources")
                    .isNotNull();
            return Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            fail("Failed to load test resource", e);
            return null;
        }
    }

    @Test
    void createExtractSameFiles() throws Exception {
        // Create multiple source files
        var sourceFile1 = createFile(tempDir, "file1.txt", "Content of file 1");
        var sourceFile2 = createFile(tempDir, "file2.txt", "Content of file 2");
        var sourceFile3 = createFile(tempDir, "data.bin", "Binary data content");

        var archivePath = tempDir.resolve("test" + getExtension());
        var extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        // Create archive
        try (ArchiveCreator<?> creator = archiveCreatorBuilder(archivePath)) {
            creator.addFile(sourceFile1);
            creator.addFile(sourceFile2);
            creator.addFile(sourceFile3);
        }

        assertThat(archivePath).exists();

        // Extract archive
        try (ArchiveExtractor<?> extractor = archiveExtractorBuilder(archivePath)) {
            extractor.extract(extractDir);
        }

        // Verify extracted files
        assertThat(extractDir.resolve("file1.txt")).exists().hasContent("Content of file 1");
        assertThat(extractDir.resolve("file2.txt")).exists().hasContent("Content of file 2");
        assertThat(extractDir.resolve("data.bin")).exists().hasContent("Binary data content");
    }

    @Test
    void createArchiveWithCustomNames() throws Exception {
        // Create source files
        var sourceFile1 = createFile(tempDir, "original1.txt", "First file content");
        var sourceFile2 = createFile(tempDir, "original2.txt", "Second file content");

        var archivePath = tempDir.resolve("custom" + getExtension());
        var extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        // Create archive with custom entry names
        try (ArchiveCreator<?> creator = archiveCreatorBuilder(archivePath)) {
            creator.addFile("renamed1.txt", sourceFile1);
            creator.addFile("renamed2.txt", sourceFile2);
        }

        assertThat(archivePath).exists();

        // Extract archive
        try (ArchiveExtractor<?> extractor = archiveExtractorBuilder(archivePath)) {
            extractor.extract(extractDir);
        }

        // Verify extracted files have custom names
        assertThat(extractDir.resolve("renamed1.txt")).exists().hasContent("First file content");
        assertThat(extractDir.resolve("renamed2.txt")).exists().hasContent("Second file content");
    }

    @Test
    void createArchiveFromByteArrays() throws Exception {
        var archivePath = tempDir.resolve("bytes" + getExtension());
        var extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        // Create archive from byte arrays
        try (ArchiveCreator<?> creator = archiveCreatorBuilder(archivePath)) {
            creator.addFile("text.txt", "Text content from bytes".getBytes());
            creator.addFile("binary.dat", new byte[] {0x01, 0x02, 0x03, (byte) 0xFF});
        }

        assertThat(archivePath).exists();

        // Extract archive
        try (ArchiveExtractor<?> extractor = archiveExtractorBuilder(archivePath)) {
            extractor.extract(extractDir);
        }

        // Verify extracted files
        assertThat(extractDir.resolve("text.txt")).exists().hasContent("Text content from bytes");
        assertThat(extractDir.resolve("binary.dat")).exists();

        byte[] extractedBinary = Files.readAllBytes(extractDir.resolve("binary.dat"));
        Assertions.assertThat(extractedBinary).isEqualTo(new byte[] {0x01, 0x02, 0x03, (byte) 0xFF});
    }

    @Test
    void extractOsCreatedArchive() throws Exception {
        // This test validates that we can extract archives created by OS tools
        var osArchive = getArchive();
        if (Files.exists(osArchive)) {
            var extractDir = tempDir.resolve("os_extracted");
            Files.createDirectories(extractDir);

            try (ArchiveExtractor<?> extractor = archiveExtractorBuilder(osArchive)) {
                extractor.extract(extractDir);

                try (var files = Files.list(extractDir)) {
                    List<Path> extractedFiles = files.toList();
                    Assertions.assertThat(extractedFiles).isNotEmpty();
                }
            } catch (IOException e) {
                fail("Invalid archive", e);
            }
        } else {
            fail("Test archive file not found: " + osArchive);
        }
    }

    @Test
    void roundTripArchive() throws Exception {
        // Create original files
        var sourceFile1 = createFile(tempDir, "round1.txt", "Round trip test 1");
        var sourceFile2 = createFile(tempDir, "round2.txt", "Round trip test 2");

        var archivePath1 = tempDir.resolve("round1" + getExtension());
        var extractDir1 = tempDir.resolve("extract1");
        var archivePath2 = tempDir.resolve("round2" + getExtension());
        var extractDir2 = tempDir.resolve("extract2");

        Files.createDirectories(extractDir1);
        Files.createDirectories(extractDir2);

        // First round: Create archive
        try (ArchiveCreator<?> creator = archiveCreatorBuilder(archivePath1)) {
            creator.addFile(sourceFile1);
            creator.addFile(sourceFile2);
        }

        // Extract first archive
        try (ArchiveExtractor<?> extractor = archiveExtractorBuilder(archivePath1)) {
            extractor.extract(extractDir1);
        }

        // Second round: Create archive from extracted files
        try (ArchiveCreator<?> creator = archiveCreatorBuilder(archivePath2)) {
            creator.addFile(extractDir1.resolve("round1.txt"));
            creator.addFile(extractDir1.resolve("round2.txt"));
        }

        // Extract second archive
        try (ArchiveExtractor<?> extractor = archiveExtractorBuilder(archivePath2)) {
            extractor.extract(extractDir2);
        }

        // Verify both extractions have same content
        assertThat(extractDir1.resolve("round1.txt")).hasSameTextualContentAs(extractDir2.resolve("round1.txt"));
        assertThat(extractDir1.resolve("round2.txt")).hasSameTextualContentAs(extractDir2.resolve("round2.txt"));
    }
}
