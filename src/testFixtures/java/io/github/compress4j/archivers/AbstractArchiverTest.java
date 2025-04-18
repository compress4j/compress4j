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

import static io.github.compress4j.test.util.FileTestUtils.assertDirectoryContentMatches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import io.github.compress4j.test.util.FileTestUtils;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractArchiverTest extends AbstractResourceTest {

    private Archiver archiver;

    private File archive;

    protected void assertExtractionWasSuccessful() {
        assertDirectoryContentMatches(ARCHIVE_DIR.toPath(), archiveExtractTmpDir.toPath());
    }

    @BeforeEach
    public void setUp() {
        archiver = getArchiver();
        archive = getArchive();
    }

    @AfterEach
    public void tearDown() {
        archiver = null;
        archive = null;
    }

    protected abstract Archiver getArchiver();

    protected abstract File getArchive();

    @Test
    void extract_properlyExtractsArchive() throws Exception {
        archiver.extract(archive, archiveExtractTmpDir);

        assertExtractionWasSuccessful();
    }

    @Test
    void extract_properlyExtractsArchiveStream() throws Exception {
        try (InputStream archiveAsStream = new FileInputStream(archive)) {
            archiver.extract(archiveAsStream, archiveExtractTmpDir);
            assertExtractionWasSuccessful();
        }
    }

    @Test
    void create_recursiveDirectory_withFileExtension_properlyCreatesArchive() throws Exception {
        String archiveName = archive.getName();

        File createdArchive = archiver.create(archiveName, archiveCreateTmpDir, ARCHIVE_DIR);

        assertThat(createdArchive).exists().hasName(archiveName);

        archiver.extract(createdArchive, archiveExtractTmpDir);
        assertExtractionWasSuccessful();
    }

    @Test
    void create_multipleSourceFiles_properlyCreatesArchive() throws Exception {
        String archiveName = archive.getName();

        File createdArchive = archiver.create(archiveName, archiveCreateTmpDir, ARCHIVE_DIR.listFiles());

        assertThat(createdArchive).exists().hasName(archiveName);

        archiver.extract(createdArchive, archiveExtractTmpDir);
        FileTestUtils.assertDirectoryContentMatches(ARCHIVE_DIR.toPath(), archiveExtractTmpDir.toPath());
    }

    @Test
    void create_recursiveDirectory_withoutFileExtension_properlyCreatesArchive() throws Exception {
        String archiveName = archive.getName();

        File actualArchive = archiver.create("archive", archiveCreateTmpDir, ARCHIVE_DIR);

        assertThat(actualArchive).exists().hasName(archiveName);

        archiver.extract(actualArchive, archiveExtractTmpDir);
        assertExtractionWasSuccessful();
    }

    @Test
    void create_withNonExistingSource_fails() {
        assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> archiver.create("archive", archiveCreateTmpDir, NON_EXISTING_FILE));
    }

    @Test
    void create_withNonReadableSource_fails() {
        assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> archiver.create("archive", archiveCreateTmpDir, nonReadableFile));
    }

    @Test
    void create_withFileAsDestination_fails() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> archiver.create("archive", nonReadableFile, ARCHIVE_DIR));
    }

    @Test
    void create_withNonWritableDestination_fails() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> archiver.create("archive", nonWritableDir, ARCHIVE_DIR));
    }

    @Test
    void extract_withNonExistingSource_fails() {
        assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> archiver.extract(NON_EXISTING_FILE, archiveExtractTmpDir));
    }

    @Test
    void extract_withNonReadableSource_fails() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> archiver.extract(nonReadableFile, archiveExtractTmpDir));
    }

    @Test
    void extract_withFileAsDestination_fails() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> archiver.extract(archive, nonReadableFile));
    }

    @Test
    void extract_withNonWritableDestination_fails() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> archiver.extract(archive, nonWritableDir));
    }

    @Test
    void stream_returnsCorrectEntries() throws IOException {
        try (ArchiveStream stream = archiver.stream(archive)) {
            ArchiveEntry entry;
            List<String> entries = new ArrayList<>();

            while ((entry = stream.getNextEntry()) != null) {
                entries.add(entry.getName().replaceAll("/$", "")); // remove trailing slashes for test compatibility
            }

            assertThat(entries)
                    .hasSize(12)
                    .contains("file.txt")
                    .contains("file.txt")
                    .contains(
                            "looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong_filename.txt")
                    .contains("folder")
                    .contains("folder/folder_file.txt")
                    .contains("folder/subfolder/subfolder_file.txt")
                    .contains("folder/subfolder")
                    .contains("permissions")
                    .contains("permissions/executable_file.txt")
                    .contains("permissions/private_executable_file.txt")
                    .contains("permissions/readonly_file.txt")
                    .contains("permissions/private_folder")
                    .contains("permissions/private_folder/private_file.txt");
        }
    }

    @Test
    void entry_isDirectory_behavesCorrectly() throws Exception {
        try (ArchiveStream stream = archiver.stream(archive)) {
            ArchiveEntry entry;

            while ((entry = stream.getNextEntry()) != null) {
                String name = entry.getName().replaceAll("/$", ""); // remove trailing slashes for test compatibility

                if (name.endsWith("folder")
                        || name.endsWith("subfolder")
                        || name.endsWith("permissions")
                        || name.endsWith("private_folder")) {
                    assertThat(entry.isDirectory())
                            .withFailMessage("<%s> is a directory", entry.getName())
                            .isTrue();
                } else {
                    assertThat(entry.isDirectory())
                            .withFailMessage("<%s> is not a directory", entry.getName())
                            .isFalse();
                }
            }
        }
    }

    @Test
    void entry_geSize_behavesCorrectly() throws Exception {
        try (ArchiveStream stream = archiver.stream(archive)) {
            ArchiveEntry entry;

            while ((entry = stream.getNextEntry()) != null) {
                String name = entry.getName().replaceAll("/$", ""); // remove trailing slashes for test compatibility

                if (name.endsWith("folder")
                        || name.endsWith("subfolder")
                        || name.endsWith("permissions")
                        || name.endsWith("private_folder")) {
                    assertThat(entry.getSize()).isZero();
                } else {
                    assertThat(entry.getSize()).isNotZero();
                }
            }
        }
    }

    @Test
    void entry_getLastModifiedDate_behavesCorrectly() throws Exception {
        try (ArchiveStream stream = archiver.stream(archive)) {
            ArchiveEntry entry;

            while ((entry = stream.getNextEntry()) != null) {
                assertThat(entry.getLastModifiedDate()).isNotNull();
                assertThat(entry.getLastModifiedDate())
                        .withFailMessage("modification date should be before now")
                        .isBefore(new Date());
            }
        }
    }

    @Test
    void stream_extractEveryEntryWorks() throws Exception {
        try (ArchiveStream stream = archiver.stream(archive)) {
            ArchiveEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                entry.extract(archiveExtractTmpDir);
            }
        }

        assertExtractionWasSuccessful();
    }

    @Test
    void stream_extractPassedEntry_throwsException() throws Exception {
        try (ArchiveStream stream = archiver.stream(archive)) {
            ArchiveEntry entry = stream.getNextEntry();
            Assertions.assertDoesNotThrow(stream::getNextEntry, "Illegal state exception caught to early");
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> entry.extract(archiveExtractTmpDir));
        }
    }

    @Test
    void stream_extractOnClosedStream_throwsException() throws Exception {
        ArchiveEntry entry = null;
        try (ArchiveStream stream = archiver.stream(archive)) {
            entry = stream.getNextEntry();
        } catch (IllegalStateException e) {
            fail("Illegal state exception caught too early");
        }

        ArchiveEntry finalEntry = entry;
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> finalEntry.extract(archiveExtractTmpDir));
    }
}
