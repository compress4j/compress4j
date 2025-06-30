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
package io.github.compress4j.archivers;

import static io.github.compress4j.utils.DependencyCheckerTestConstants.EXPECTED_MESSAGE_LZMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.github.compress4j.exceptions.MissingArchiveDependencyException;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("java:S5778")
class Archiver7zDependencyCheckerTest {

    @TempDir
    protected File archiveTmpDir;

    @SuppressWarnings("resource")
    @Test
    void shouldCheckLZMADependencyWhenArchiverStream() throws IOException {
        Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.SEVEN_Z);

        try {
            archiver.stream(new File(archiveTmpDir, "file.7z"));
            fail("Expected MissingArchiveDependencyException");
        } catch (MissingArchiveDependencyException e) {
            assertThat(e).hasMessage(EXPECTED_MESSAGE_LZMA);
        }
    }

    @Test
    void shouldCheckLZMADependencyWhenArchiverCreate() throws IOException {
        Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.SEVEN_Z);

        try {
            archiver.create("", new File(archiveTmpDir, "file.7z"));
            fail("Expected MissingArchiveDependencyException");
        } catch (MissingArchiveDependencyException e) {
            assertThat(e).hasMessage(EXPECTED_MESSAGE_LZMA);
        }
    }
}
