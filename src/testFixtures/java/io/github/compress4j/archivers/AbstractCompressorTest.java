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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5778")
public abstract class AbstractCompressorTest extends AbstractResourceTest {

    public static final File COMPRESS_TXT = new File(RESOURCES_DIR, "compress.txt");
    public static final File COMPRESS_UNKNOWN = new File(RESOURCES_DIR, "compress.txt.unknown");

    private File decompressDestinationFile;
    private File compressDestinationFile;

    @BeforeEach
    public void setUp() {
        decompressDestinationFile = new File(archiveExtractTmpDir, "compress.txt");
        compressDestinationFile =
                new File(archiveCreateTmpDir, "compress.txt" + getCompressor().getFilenameExtension());
    }

    protected abstract File getCompressedFile();

    protected abstract CompressionType getCompressionType();

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
    public void compress_Directory_throwsException() {
        var exception = assertThrows(
                IllegalArgumentException.class, () -> getCompressor().compress(RESOURCES_DIR, compressDestinationFile));
        assertThat(exception).hasMessage("Source src/test/resources is a directory.");
    }

    @Test
    public void compress_nullFile_throwsException() {
        var exception = assertThrows(
                IllegalArgumentException.class, () -> getCompressor().compress(null, compressDestinationFile));
        assertThat(exception).hasMessage("Source is null");
    }

    @Test
    public void compress_nonReadableFile_throwsException() {
        try {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> getCompressor().compress(nonReadableFile, compressDestinationFile));
        } finally {
            assertThat(compressDestinationFile).doesNotExist();
        }
    }

    @Test
    public void compress_nonExistingFile_throwsException() {
        try {
            assertThatExceptionOfType(FileNotFoundException.class)
                    .isThrownBy(() -> getCompressor().compress(NON_EXISTING_FILE, compressDestinationFile));
        } finally {
            assertThat(compressDestinationFile).doesNotExist();
        }
    }

    @Test
    public void compress_withNullDestination_throwsException() {
        var exception = assertThrows(
                IllegalArgumentException.class, () -> getCompressor().compress(COMPRESS_TXT, null));
        assertThat(exception).hasMessage("Destination is null");
    }

    @Test
    public void compress_withNonExistingDestination_throwsException() {
        assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> getCompressor().compress(COMPRESS_TXT, NON_EXISTING_FILE));
    }

    @Test
    public void compress_withNonWritableDestinationFile_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> getCompressor().compress(COMPRESS_TXT, nonWritableFile));
    }

    @Test
    public void compress_withNonWritableDestinationDirectory_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> getCompressor().compress(COMPRESS_TXT, nonWritableDir));
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
    public void decompress_Directory_throwsException() {
        var exception = assertThrows(
                IllegalArgumentException.class, () -> getCompressor().decompress(RESOURCES_DIR, archiveExtractTmpDir));
        assertThat(exception).hasMessage("Source src/test/resources is a directory.");
    }

    @Test
    public void decompress_nullFile_throwsException() {
        var exception = assertThrows(
                IllegalArgumentException.class, () -> getCompressor().decompress(null, archiveExtractTmpDir));
        assertThat(exception).hasMessage("Source is null");
    }

    @Test
    public void decompress_unknownFileType_throwsException() {
        var exception = assertThrows(IllegalArgumentException.class, () -> getCompressor()
                .decompress(COMPRESS_UNKNOWN, archiveExtractTmpDir));
        assertThat(exception)
                .hasMessage("src/test/resources/compress.txt.unknown is not of type " + getCompressionType());
    }

    @Test
    public void decompress_withNonExistingDestination_throwsException() {
        assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> getCompressor().decompress(getCompressedFile(), NON_EXISTING_FILE));
    }

    @Test
    public void decompress_withNonWritableDestinationFile_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> getCompressor().decompress(getCompressedFile(), nonWritableFile));
    }

    @Test
    public void decompress_withNonWritableDestinationDirectory_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> getCompressor().decompress(getCompressedFile(), nonWritableDir));
    }

    @Test
    public void decompress_withNullDestinationDirectory_throwsException() {
        var exception = assertThrows(
                IllegalArgumentException.class, () -> getCompressor().decompress(getCompressedFile(), null));
        assertThat(exception).hasMessage("Destination is null");
    }

    @Test
    public void decompress_nonExistingFile_throwsException() {
        assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> getCompressor().decompress(NON_EXISTING_FILE, decompressDestinationFile));
    }

    private void assertCompressionWasSuccessful() throws Exception {
        getCompressor().decompress(compressDestinationFile, decompressDestinationFile);
        assertDecompressionWasSuccessful();
    }

    private void assertDecompressionWasSuccessful() {
        assertThat(decompressDestinationFile).exists().hasSameTextualContentAs(COMPRESS_TXT);
    }
}
