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

import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.compress4j.archivers.AbstractArchiverIntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for TAR archive functionality.
 *
 * @since 2.2
 */
class TarArchiveIntegrationTest extends AbstractArchiverIntegrationTest {

    @Override
    protected TarArchiveCreator archiveCreatorBuilder(Path archivePath) throws IOException {
        return TarArchiveCreator.builder(Files.newOutputStream(archivePath)).build();
    }

    @Override
    protected TarArchiveExtractor archiveExtractorBuilder(Path archivePath) throws IOException {
        return TarArchiveExtractor.builder(Files.newInputStream(archivePath)).build();
    }

    @Override
    protected String getExtension() {
        return ".tar";
    }

    @Test
    void testTarSpecificFeatures() throws Exception {
        // Test TAR-specific features like preserving file permissions
        var sourceFile = tempDir.resolve("executable.sh");
        Files.write(sourceFile, "#!/bin/bash\necho 'Hello World'".getBytes());

        var archivePath = tempDir.resolve("tar-features.tar");
        var extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        // Create archive with TAR features
        try (var creator = TarArchiveCreator.builder(Files.newOutputStream(archivePath))
                .longFileMode(LONGFILE_GNU)
                .build()) {
            creator.addFile("executable.sh", sourceFile);
        }

        // Extract and verify
        try (var extractor =
                TarArchiveExtractor.builder(Files.newInputStream(archivePath)).build()) {
            extractor.extract(extractDir);
        }

        assertThat(extractDir.resolve("executable.sh")).exists().hasContent("#!/bin/bash\necho 'Hello World'");
    }
}
