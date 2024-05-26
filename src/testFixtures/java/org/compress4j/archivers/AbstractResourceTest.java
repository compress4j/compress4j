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
package org.compress4j.archivers;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class AbstractResourceTest {

    protected static final File RESOURCES_DIR = new File("src/test/resources");

    /**
     * Contains the following files:
     *
     * <ul>
     *   <li>src/test/resources/archives/archive/folder
     *   <li>src/test/resources/archives/archive/folder/folder_file.txt
     *   <li>src/test/resources/archives/archive/folder/subfolder
     *   <li>src/test/resources/archives/archive/folder/subfolder/subfolder_file.txt
     *   <li>src/test/resources/archives/archive/file.txt
     * </ul>
     *
     * <br>
     * Used both as reference to compare whether extraction was successful, and used as source for compression tests.
     */
    public static final File ARCHIVE_DIR = new File(RESOURCES_DIR, "archive");

    protected static final File NON_EXISTING_FILE = new File(RESOURCES_DIR, "some/file/that/does/not/exist");

    @SuppressWarnings("java:S2924")
    @TempDir
    protected File archiveTmpDir;

    @SuppressWarnings("java:S2924")
    @TempDir
    protected File archiveCreateTmpDir;

    @SuppressWarnings("java:S2924")
    @TempDir
    protected File archiveExtractTmpDir;

    protected File nonReadableFile;
    protected File nonWritableFile;
    protected File nonReadableDir;
    protected File nonWritableDir;

    private static void setupNonExistingFile() {
        if (NON_EXISTING_FILE.exists()) {
            NON_EXISTING_FILE.setWritable(true);
            NON_EXISTING_FILE.setReadable(true);
            NON_EXISTING_FILE.delete();
        }
    }

    @BeforeEach
    public synchronized void createResources() throws IOException {
        nonReadableFile = new File(archiveTmpDir, "non_readable_file.txt");
        nonWritableFile = new File(archiveTmpDir, "non_writable_file.txt");
        nonReadableDir = new File(archiveTmpDir, "non_readable_dir");
        nonWritableDir = new File(archiveTmpDir, "non_writable_dir");
        setupNonExistingFile();

        nonReadableFile.createNewFile();
        nonReadableFile.setReadable(false);

        nonWritableFile.createNewFile();
        nonWritableFile.setWritable(false);

        nonReadableDir.mkdir();
        nonReadableDir.setReadable(false);

        nonWritableDir.mkdir();
        nonWritableDir.setWritable(false);
    }
}
