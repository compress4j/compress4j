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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class AbstractResourceTest {

    public static final File RESOURCES_DIR = new File("src/test/resources");

    public static final File ARCHIVE_CREATE_DIR = new File(RESOURCES_DIR, "created");
    public static final File ARCHIVE_EXTRACT_DIR = new File(RESOURCES_DIR, "extracted");

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

    public static final File NON_READABLE_FILE = new File(RESOURCES_DIR, "non_readable_file.txt");
    public static final File NON_WRITABLE_FILE = new File(RESOURCES_DIR, "non_writable_file.txt");
    public static final File NON_READABLE_DIR = new File(RESOURCES_DIR, "non_readable_dir");
    public static final File NON_WRITABLE_DIR = new File(RESOURCES_DIR, "non_writable_dir");

    public static final File NON_EXISTING_FILE = new File(RESOURCES_DIR, "some/file/that/does/not/exist");

    @SuppressWarnings("SameParameterValue")
    protected static void assertDirectoryStructureEquals(File expected, File actual) throws IOException {
        List<String> expectedFiles = flatRelativeList(expected);
        List<String> actualFiles = flatRelativeList(actual);

        assertThat(expectedFiles)
                .withFailMessage("Directory structures of <%s> and <%s> do not match.", expected, actual)
                .hasSameElementsAs(actualFiles);
    }

    public static void assertFileContentEquals(File expected, File actual) throws IOException {
        String actualString = new Scanner(actual).useDelimiter("\\Z").next();
        String expectedString = new Scanner(expected).useDelimiter("\\Z").next();

        assertThat(actualString)
                .withFailMessage("File contents of <%s> and <%s> differ.", expected, actual)
                .isEqualTo(expectedString);
    }

    @SuppressWarnings("SameParameterValue")
    protected static void assertFilesEquals(File expectedDir, File actualDir) throws Exception {
        String[] expectedFiles = flatArray(expectedDir);
        String[] actualFiles = flatArray(actualDir);

        Arrays.sort(expectedFiles);
        Arrays.sort(actualFiles);

        // check whether hashes of files match
        for (int i = 0; i < expectedFiles.length; i++) {
            File expected = new File(expectedFiles[i]);
            File actual = new File(actualFiles[i]);

            assertThat(actual).hasName(expected.getName());

            if (expected.isFile()) {
                assertFileContentEquals(expected, actual);
            }
        }
    }

    public static String[] flatArray(File root) {
        List<String> flatList = flatList(root);

        return flatList.toArray(new String[0]);
    }

    public static List<String> flatList(File root) {
        List<String> list = new ArrayList<>();

        File[] nodes = root.listFiles();

        assert nodes != null;
        for (File node : nodes) {
            list.add(node.getPath());

            if (node.isDirectory()) {
                list.addAll(flatList(node));
            }
        }

        return list;
    }

    public static List<String> flatRelativeList(File root) throws IOException {
        return flatRelativeList(root, root);
    }

    public static List<String> flatRelativeList(File root, File current) throws IOException {
        List<String> list = new ArrayList<>();

        String prefix = root.getCanonicalPath();
        File[] nodes = current.getCanonicalFile().listFiles();

        assert nodes != null;
        for (File node : nodes) {
            list.add(node.getPath().substring(prefix.length() + 1));

            if (node.isDirectory()) {
                list.addAll(flatRelativeList(root, node));
            }
        }

        return list;
    }

    public static void useDirectory(File dir) {
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    public static void useFile(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public static void free(File file) throws IOException {
        if (file.exists()) {
            file.setWritable(true);
            file.setReadable(true);
            remove(file);
        }
    }

    protected static void remove(File file) throws IOException {
        if (file.isDirectory()) {
            removeDirectory(file);
        } else {
            file.delete();
        }
    }

    protected static void removeDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            remove(file);
        }

        directory.delete();
    }

    @BeforeEach
    public synchronized void createResources() throws IOException {
        useDirectory(ARCHIVE_EXTRACT_DIR);
        useDirectory(ARCHIVE_CREATE_DIR);

        free(NON_EXISTING_FILE);

        useFile(NON_READABLE_FILE);
        NON_READABLE_FILE.setReadable(false);

        useFile(NON_WRITABLE_FILE);
        NON_WRITABLE_FILE.setWritable(false);

        useDirectory(NON_READABLE_DIR);
        NON_READABLE_DIR.setReadable(false);

        useDirectory(NON_WRITABLE_DIR);
        NON_WRITABLE_DIR.setWritable(false);
    }

    @AfterEach
    public synchronized void dropResources() throws IOException {
        free(ARCHIVE_EXTRACT_DIR);
        free(ARCHIVE_CREATE_DIR);
        free(NON_READABLE_FILE);
        free(NON_WRITABLE_FILE);
        free(NON_READABLE_DIR);
        free(NON_WRITABLE_DIR);
    }
}
