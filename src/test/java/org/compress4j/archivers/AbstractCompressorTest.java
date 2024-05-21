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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ResultOfMethodCallIgnored")
public abstract class AbstractCompressorTest extends AbstractResourceTest {

    private final File original = new File(RESOURCES_DIR, "compress.txt");

    private final File decompressDestinationDir = ARCHIVE_EXTRACT_DIR;
    private final File compressDestinationDir = ARCHIVE_CREATE_DIR;
    private File decompressDestinationFile;
    private File compressDestinationFile;

    private Compressor compressor;
    private File compressedFile;

    @BeforeEach
    public void setUp() {
        compressor = getCompressor();
        compressedFile = getCompressedFile();

        decompressDestinationFile = new File(decompressDestinationDir, "compress.txt");
        compressDestinationFile = new File(compressDestinationDir, "compress.txt" + compressor.getFilenameExtension());
    }

    @AfterEach
    public void tearDown() {
        compressor = null;
        compressedFile = null;

        if (decompressDestinationFile.exists()) {
            decompressDestinationFile.delete();
        }
        if (compressDestinationFile.exists()) {
            compressDestinationFile.delete();
        }

        decompressDestinationFile = null;
        compressDestinationFile = null;
    }

    protected abstract File getCompressedFile();

    protected abstract Compressor getCompressor();

    @Test
    public void compress_withFileDestination_compressesFileCorrectly() throws Exception {
        compressor.compress(original, compressDestinationFile);

        assertCompressionWasSuccessful();
    }

    @Test
    public void compress_withDirectoryDestination_compressesFileCorrectly() throws Exception {
        compressor.compress(original, compressDestinationDir);

        assertCompressionWasSuccessful();
    }

    @Test
    public void compress_nonReadableFile_throwsException() {
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> compressor.compress(NON_READABLE_FILE, compressDestinationFile));
        } finally {
            assertFalse(compressDestinationFile.exists());
        }
    }

    @Test
    public void compress_nonExistingFile_throwsException() {
        try {
            assertThrows(
                    FileNotFoundException.class, () -> compressor.compress(NON_EXISTING_FILE, compressDestinationFile));
        } finally {
            assertFalse(compressDestinationFile.exists());
        }
    }

    @Test
    public void compress_withNonExistingDestination_throwsException() {
        assertThrows(FileNotFoundException.class, () -> compressor.compress(original, NON_EXISTING_FILE));
    }

    @Test
    public void compress_withNonWritableDestinationFile_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> compressor.compress(original, NON_WRITABLE_FILE));
    }

    @Test
    public void compress_withNonWritableDestinationDirectory_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> compressor.compress(original, NON_WRITABLE_DIR));
    }

    @Test
    public void decompress_withFileDestination_decompressesFileCorrectly() throws Exception {
        compressor.decompress(compressedFile, decompressDestinationFile);

        assertDecompressionWasSuccessful();
    }

    @Test
    public void decompress_withDirectoryDestination_decompressesFileCorrectly() throws Exception {
        compressor.decompress(compressedFile, decompressDestinationDir);

        assertDecompressionWasSuccessful();
    }

    @Test
    public void decompress_withNonExistingDestination_throwsException() {
        assertThrows(FileNotFoundException.class, () -> compressor.decompress(compressedFile, NON_EXISTING_FILE));
    }

    @Test
    public void decompress_withNonWritableDestinationFile_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> compressor.decompress(compressedFile, NON_WRITABLE_FILE));
    }

    @Test
    public void decompress_withNonWritableDestinationDirectory_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> compressor.decompress(compressedFile, NON_WRITABLE_DIR));
    }

    @Test
    public void decompress_nonExistingFile_throwsException() {
        assertThrows(
                FileNotFoundException.class, () -> compressor.decompress(NON_EXISTING_FILE, decompressDestinationFile));
    }

    private void assertCompressionWasSuccessful() throws Exception {
        assertThat(compressDestinationFile).exists();
        compressor.decompress(compressDestinationFile, decompressDestinationFile);
        assertDecompressionWasSuccessful();
    }

    private void assertDecompressionWasSuccessful() throws Exception {
        assertThat(decompressDestinationFile).exists();
        assertFileContentEquals(original, decompressDestinationFile);
    }
}
