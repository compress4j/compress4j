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

import io.github.compress4j.archivers.AbstractArchiverTest;
import io.github.compress4j.archivers.ArchiveCreator;
import io.github.compress4j.archivers.ArchiveExtractor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * End-to-end tests for TAR.BZ2 archive functionality.
 *
 * @since 2.2
 */
class TarBZip2ArchiveTest extends AbstractArchiverTest {

    @Override
    protected ArchiveCreator<?> archiveCreatorBuilder(Path archivePath) throws IOException {
        return TarBZip2ArchiveCreator.builder(Files.newOutputStream(archivePath))
                .build();
    }

    @Override
    protected ArchiveExtractor<?> archiveExtractorBuilder(Path archivePath) throws IOException {
        return TarBZip2ArchiveExtractor.builder(Files.newInputStream(archivePath))
                .build();
    }

    @Override
    protected String archiveExtension() {
        return ".tar.bz2";
    }

    @Override
    @DisabledOnOs(OS.WINDOWS)
    protected Path osArchivedPath() {
        return Path.of("src/e2eTest/resources/archives/test.tar.bz2");
    }

    @Test
    void testBZip2CompressionFeatures() throws Exception {
        // Create multiple files with different characteristics for BZip2 compression
        var textFile = tempDir.resolve("text.txt");
        var binaryFile = tempDir.resolve("binary.dat");

        Files.write(textFile, "Text content with patterns patterns patterns".getBytes());

        // Create binary data
        byte[] binaryData = new byte[2048];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte) (i % 256);
        }
        Files.write(binaryFile, binaryData);

        var archivePath = tempDir.resolve("bzip2-test.tar.bz2");
        var extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        // Create BZip2 compressed archive
        try (var creator = TarBZip2ArchiveCreator.builder(Files.newOutputStream(archivePath))
                .build()) {
            creator.addFile("text.txt", textFile);
            creator.addFile("binary.dat", binaryFile);
        }

        // Extract and verify
        try (var extractor = TarBZip2ArchiveExtractor.builder(Files.newInputStream(archivePath))
                .build()) {
            extractor.extract(extractDir);
        }

        assertThat(extractDir.resolve("text.txt")).exists().hasContent("Text content with patterns patterns patterns");
        assertThat(extractDir.resolve("binary.dat")).exists().hasBinaryContent(binaryData);
    }
}
