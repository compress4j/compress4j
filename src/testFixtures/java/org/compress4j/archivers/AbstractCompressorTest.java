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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5778")
public abstract class AbstractCompressorTest extends AbstractResourceTest {

    public static final File COMPRESS_TXT = new File(RESOURCES_DIR, "compress.txt");

    private File decompressDestinationFile;
    private File compressDestinationFile;

    @BeforeEach
    public void setUp() {
        decompressDestinationFile = new File(archiveExtractTmpDir, "compress.txt");
        compressDestinationFile =
                new File(archiveCreateTmpDir, "compress.txt" + getCompressor().getFilenameExtension());
    }

    protected abstract File getCompressedFile();

    protected abstract Compressor getCompressor();

    @Test
    public void compress_withFileDestination_compressesFileCorrectly() throws Exception {
        getCompressor().compress(COMPRESS_TXT, compressDestinationFile);

        assertCompressionWasSuccessful();
    }

    @Test
    public void compress_withDirectoryDestination_compressesFileCorrectly() throws Exception {
        getCompressor().compress(COMPRESS_TXT, archiveCreateTmpDir);

        assertCompressionWasSuccessful();
    }

    @Test
    public void compress_nonReadableFile_throwsException() {
        try {
            assertThrows(IllegalArgumentException.class, () -> getCompressor()
                    .compress(nonReadableFile, compressDestinationFile));
        } finally {
            assertFalse(compressDestinationFile.exists());
        }
    }

    @Test
    public void compress_nonExistingFile_throwsException() {
        try {
            assertThrows(FileNotFoundException.class, () -> getCompressor()
                    .compress(NON_EXISTING_FILE, compressDestinationFile));
        } finally {
            assertFalse(compressDestinationFile.exists());
        }
    }

    @Test
    public void compress_withNonExistingDestination_throwsException() {
        assertThrows(FileNotFoundException.class, () -> getCompressor().compress(COMPRESS_TXT, NON_EXISTING_FILE));
    }

    @Test
    public void compress_withNonWritableDestinationFile_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> getCompressor().compress(COMPRESS_TXT, nonWritableFile));
    }

    @Test
    public void compress_withNonWritableDestinationDirectory_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> getCompressor().compress(COMPRESS_TXT, nonWritableDir));
    }

    @Test
    public void decompress_withFileDestination_decompressesFileCorrectly() throws Exception {
        getCompressor().decompress(getCompressedFile(), decompressDestinationFile);

        assertDecompressionWasSuccessful();
    }

    @Test
    public void decompress_withDirectoryDestination_decompressesFileCorrectly() throws Exception {
        getCompressor().decompress(getCompressedFile(), archiveExtractTmpDir);

        assertDecompressionWasSuccessful();
    }

    @Test
    public void decompress_withNonExistingDestination_throwsException() {
        assertThrows(
                FileNotFoundException.class, () -> getCompressor().decompress(getCompressedFile(), NON_EXISTING_FILE));
    }

    @Test
    public void decompress_withNonWritableDestinationFile_throwsException() {
        assertThrows(
                IllegalArgumentException.class, () -> getCompressor().decompress(getCompressedFile(), nonWritableFile));
    }

    @Test
    public void decompress_withNonWritableDestinationDirectory_throwsException() {
        assertThrows(
                IllegalArgumentException.class, () -> getCompressor().decompress(getCompressedFile(), nonWritableDir));
    }

    @Test
    public void decompress_nonExistingFile_throwsException() {
        assertThrows(FileNotFoundException.class, () -> getCompressor()
                .decompress(NON_EXISTING_FILE, decompressDestinationFile));
    }

    private void assertCompressionWasSuccessful() throws Exception {
        getCompressor().decompress(compressDestinationFile, decompressDestinationFile);
        assertDecompressionWasSuccessful();
    }

    private void assertDecompressionWasSuccessful() {
        assertThat(decompressDestinationFile).exists().hasSameTextualContentAs(COMPRESS_TXT);
    }
}
