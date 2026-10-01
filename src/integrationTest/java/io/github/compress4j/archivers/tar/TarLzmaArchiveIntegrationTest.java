/*
 * Copyright 2026 The Compress4J Project
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

import io.github.compress4j.archivers.AbstractArchiverIntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class TarLzmaArchiveIntegrationTest extends AbstractArchiverIntegrationTest {

    @Override
    protected TarLzmaArchiveCreator archiveCreatorBuilder(Path archivePath) throws IOException {
        return TarLzmaArchiveCreator.builder(Files.newOutputStream(archivePath)).build();
    }

    @Override
    protected TarLzmaArchiveExtractor archiveExtractorBuilder(Path archivePath) throws IOException {
        return TarLzmaArchiveExtractor.builder(Files.newInputStream(archivePath))
                .build();
    }

    @Override
    protected String getExtension() {
        return ".tar.lzma";
    }
}
