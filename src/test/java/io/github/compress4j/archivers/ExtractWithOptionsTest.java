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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.Test;

class ExtractWithOptionsTest extends AbstractResourceTest {

    /**
     * Contains 1 file: 1- overwrite-test.txt
     *
     * <p>overwrite-test.txt is a file located at the root of the target directory with the content as "old content"
     */
    private static final String ORIGINAL_ZIP_FILE = "extract_with_options_original.zip";

    /**
     * Contains 1 file: 1- overwrite-test.txt
     *
     * <p>overwrite-test.txt is a file located at the root of the target directory with the content as "new content"
     */
    private static final String UPDATED_ZIP_FILE = "extract_with_options_updated.zip";

    private static final String ZIP_FILE_NAME = "overwrite-test.txt";

    protected Archiver getArchiver() {
        return ArchiverFactory.createArchiver(ArchiveFormat.ZIP);
    }

    protected File getArchiveOriginal() {
        return new File(RESOURCES_DIR, ORIGINAL_ZIP_FILE);
    }

    protected File getArchiveUpdated() {
        return new File(RESOURCES_DIR, UPDATED_ZIP_FILE);
    }

    @Test
    void extract_without_options_must_fail() throws IOException {
        // Extract original file
        getArchiver().extract(getArchiveOriginal(), archiveExtractTmpDir);
        // Validate that file was extracted and contains the expected content
        assertFileContains("old content");

        // Try to extract the updated file, but it must fail as no CopyOptions
        // were passed to overwrite files
        assertThatExceptionOfType(FileAlreadyExistsException.class)
                .isThrownBy(() -> getArchiver().extract(getArchiveUpdated(), archiveExtractTmpDir));
    }

    @Test
    void extract_with_options_replace() throws IOException {
        // Extract original file
        getArchiver().extract(getArchiveOriginal(), archiveExtractTmpDir);
        // Validate that the file was extracted and contains the expected content
        assertFileContains("old content");

        // Extract the updated file with REPLACE_EXISTING option
        getArchiver().extract(getArchiveUpdated(), archiveExtractTmpDir, StandardCopyOption.REPLACE_EXISTING);
        // Validate that the file was extracted and contains the expected updated content
        assertFileContains("new content");
    }

    private void assertFileContains(String expectedFileContent) throws IOException {
        assertThat(archiveExtractTmpDir)
                .isDirectoryContaining(file -> file.getName().equals(ZIP_FILE_NAME));
        String fileContent = Files.readString(new File(archiveExtractTmpDir, ZIP_FILE_NAME).toPath());
        assertThat(fileContent).contains(expectedFileContent);
    }
}
