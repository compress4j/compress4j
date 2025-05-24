/*
 * Copyright 2024-2025 The Compress4J Project
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
package io.github.compress4j.archive.tar;

import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;

import io.github.compress4j.archive.tar.TarBZip2ArchiveCreator.TarBZip2ArchiveCreatorBuilder;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;

class TarBZip2ArchiveCreatorIntegrationTest extends TarArchiveCreatorIntegrationTest {

    @Override
    @BeforeEach
    void setup() throws IOException {
        archiveFile = tempDir.resolve("test.tar.bzip2");
        archive =
                new TarBZip2ArchiveCreator(new TarBZip2ArchiveCreatorBuilder(archiveFile).longFileMode(LONGFILE_POSIX));
    }

    @Override
    protected void extract(Path in, Path out) throws IOException {
        try (TarBZip2ArchiveExtractor tarBZip2Decompressor =
                TarBZip2ArchiveExtractor.builder(in).build()) {
            tarBZip2Decompressor.extract(out);
        }
    }
}
