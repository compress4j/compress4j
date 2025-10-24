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

import io.github.compress4j.archivers.AbstractArchiverIntegrationTest;
import io.github.compress4j.archivers.ArchiveCreator;
import io.github.compress4j.archivers.ArchiveExtractor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for TAR.GZ archive functionality.
 *
 * @since 2.2
 */
class TarGzArchiveIntegrationTest extends AbstractArchiverIntegrationTest {

    @Override
    protected ArchiveCreator<?> archiveCreatorBuilder(Path archivePath) throws IOException {
        return TarGzArchiveCreator.builder(Files.newOutputStream(archivePath)).build();
    }

    @Override
    protected ArchiveExtractor<?> archiveExtractorBuilder(Path archivePath) throws IOException {
        return TarGzArchiveExtractor.builder(Files.newInputStream(archivePath)).build();
    }

    @Override
    protected String getExtension() {
        return ".tar.gz";
    }

    @Test
    void testCompressionEfficiency() throws Exception {
        // Create a large file with repetitive content to test compression
        var sourceFile = tempDir.resolve("large.txt");
        var content = "This is a repetitive line for testing compression efficiency.\n".repeat(1000);
        Files.write(sourceFile, content.getBytes());

        var archivePath = tempDir.resolve("compressed.tar.gz");
        var extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        // Create compressed archive
        try (var creator =
                TarGzArchiveCreator.builder(Files.newOutputStream(archivePath)).build()) {
            creator.addFile("large.txt", sourceFile);
        }

        // Verify archive is smaller than source
        assertThat(Files.size(archivePath)).isLessThan(Files.size(sourceFile));

        // Extract and verify content integrity
        try (var extractor =
                TarGzArchiveExtractor.builder(Files.newInputStream(archivePath)).build()) {
            extractor.extract(extractDir);
        }

        assertThat(extractDir.resolve("large.txt")).exists().hasContent(content);
    }
}
