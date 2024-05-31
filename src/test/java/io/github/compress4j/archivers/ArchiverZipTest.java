/*
 * Copyright 2024 The Compress4J Project
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

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ArchiverZipTest extends AbstractArchiverTest {

    /**
     * Contains 2 files: 1- safe.txt 2- ../../../../../../../../../../../../../../../../../../../../../../../../../../
     * ../../../../../../../../../../../../../../../tmp/unsafe.txt
     *
     * <p>safe.txt is a safe file located at the root of the target directory and unsafe.txt that attempts to traverse
     * the tree all the way to / and down to tmp. This should be placed at target/tmp/unsafe.txt when extracted
     */
    private static final String ZIP_TRAVERSAL_FILE_1 = "zip_traversal.zip";

    /**
     * Contains 2 files: 1- safe.txt 2- ../../../unsafe.txt
     *
     * <p>safe.txt is a safe file located at the root of the target directory and unsafe.txt that attempts to traverse
     * the tree outside the target directory but not high enough to make it to /. This should be placed at
     * target/unsafe.txt when extracted
     */
    private static final String ZIP_TRAVERSAL_FILE_2 = "zip_traversal_2.zip";

    /**
     * Contains 2 files: 1- safe.txt 2- subDirectory/../../../../../../../../../../../../../../../../../../../../../
     * ../../../../../../../../../../../../../../../../../../../../../tmp/unsafe.txt
     *
     * <p>safe.txt is a safe file located at the root of the target directory and unsafe.txt that attempts to traverse
     * the tree all the way to / and down to tmp. This should be placed at target/tmp/unsafe.txt when extracted. The
     * difference between this file and ZIP_TRAVERSAL_FILE_1 is that the unsafe file relative path is not normalized.
     */
    private static final String ZIP_TRAVERSAL_FILE_3 = "zip_traversal_3.zip";

    @Override
    protected Archiver getArchiver() {
        return ArchiverFactory.createArchiver(ArchiveFormat.ZIP);
    }

    @Override
    protected File getArchive() {
        return new File(RESOURCES_DIR, "archive.zip");
    }

    @Test
    void zip_traversal_test_entry_extraction() throws Exception {
        archiveExtractorHelper(ZIP_TRAVERSAL_FILE_1);
        assertZipTraversal();
    }

    @Test
    void zip_traversal_test_archiver_extraction() throws Exception {
        File archive = new File(RESOURCES_DIR, ZIP_TRAVERSAL_FILE_1);
        getArchiver().extract(archive, archiveExtractTmpDir);
        assertZipTraversal();
    }

    @Test
    void zip_traversal_test_entry_extraction_target_directory_as_root() throws Exception {
        archiveExtractorHelper(ZIP_TRAVERSAL_FILE_2);
        assertTargetDirectoryAsRoot();
    }

    @Test
    void zip_traversal_test_archiver_extraction_target_directory_as_root() throws Exception {
        File archive = new File(RESOURCES_DIR, ZIP_TRAVERSAL_FILE_2);
        getArchiver().extract(archive, archiveExtractTmpDir);
        assertTargetDirectoryAsRoot();
    }

    @Test
    void zip_traversal_test_entry_extraction_for_non_normalized_path() throws Exception {
        archiveExtractorHelper(ZIP_TRAVERSAL_FILE_3);
        assertZipTraversal();
    }

    @Test
    void zip_traversal_test_archiver_extraction_for_non_normalized_path() throws Exception {
        File archive = new File(RESOURCES_DIR, ZIP_TRAVERSAL_FILE_3);
        getArchiver().extract(archive, archiveExtractTmpDir);
        assertZipTraversal();
    }

    private void archiveExtractorHelper(final String fileName) throws IOException {
        File archive = new File(RESOURCES_DIR, fileName);
        try (ArchiveStream stream = getArchiver().stream(archive)) {
            ArchiveEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                entry.extract(archiveExtractTmpDir);
            }
        }
    }

    private void assertZipTraversal() {
        assertThat(archiveExtractTmpDir)
                .isDirectoryContaining(file -> file.getName().equals("safe.txt"))
                .isDirectoryContaining(file -> file.getName().equals("tmp"))
                .isDirectoryNotContaining(file -> file.getName().equals("unsafe.txt"));
    }

    private void assertTargetDirectoryAsRoot() {
        assertThat(archiveExtractTmpDir)
                .isDirectoryContaining(file -> file.getName().equals("safe.txt"))
                .isDirectoryContaining(file -> file.getName().equals("unsafe.txt"));
    }
}
