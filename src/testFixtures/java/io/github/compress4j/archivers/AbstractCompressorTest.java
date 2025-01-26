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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

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

    @DisabledOnOs(OS.WINDOWS)
    @Test
    public void compress_Directory_throwsException() {
        assertThatThrownBy(() -> getCompressor().compress(RESOURCES_DIR, compressDestinationFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source src/test/resources is a directory.");
    }

    @Test
    public void compress_nullFile_throwsException() {
        assertThatThrownBy(() -> getCompressor().compress(null, compressDestinationFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source is null");
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    public void compress_nonReadableFile_throwsException() {
        assertThatThrownBy(() -> getCompressor().compress(nonReadableFile, compressDestinationFile))
                .isInstanceOf(IllegalArgumentException.class);
        assertFalse(compressDestinationFile.exists());
    }

    @Test
    public void compress_nonExistingFile_throwsException() {
        assertThatThrownBy(() -> getCompressor().compress(NON_EXISTING_FILE, compressDestinationFile))
                .isInstanceOf(FileNotFoundException.class);
        assertFalse(compressDestinationFile.exists());
    }

    @Test
    public void compress_withNullDestination_throwsException() {
        assertThatThrownBy(() -> getCompressor().compress(COMPRESS_TXT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Destination is null");
    }

    @Test
    public void compress_withNonExistingDestination_throwsException() {
        assertThatThrownBy(() -> getCompressor().compress(COMPRESS_TXT, NON_EXISTING_FILE))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void compress_withNonWritableDestinationFile_throwsException() {
        assertThatThrownBy(() -> getCompressor().compress(COMPRESS_TXT, nonWritableFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    public void compress_withNonWritableDestinationDirectory_throwsException() {
        assertThatThrownBy(() -> getCompressor().compress(COMPRESS_TXT, nonWritableDir))
                .isInstanceOf(IllegalArgumentException.class);
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

    @DisabledOnOs(OS.WINDOWS)
    @Test
    public void decompress_Directory_throwsException() {
        assertThatThrownBy(() -> getCompressor().decompress(RESOURCES_DIR, archiveExtractTmpDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source src/test/resources is a directory.");
    }

    @Test
    public void decompress_nullFile_throwsException() {
        assertThatThrownBy(() -> getCompressor().decompress(null, archiveExtractTmpDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source is null");
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    public void decompress_unknownFileType_throwsException() {
        assertThatThrownBy(() -> getCompressor().decompress(COMPRESS_UNKNOWN, archiveExtractTmpDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("src/test/resources/compress.txt.unknown is not of type " + getCompressionType());
    }

    @Test
    public void decompress_withNonExistingDestination_throwsException() {
        assertThatThrownBy(() -> getCompressor().decompress(getCompressedFile(), NON_EXISTING_FILE))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void decompress_withNonWritableDestinationFile_throwsException() {
        assertThatThrownBy(() -> getCompressor().decompress(getCompressedFile(), nonWritableFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    public void decompress_withNonWritableDestinationDirectory_throwsException() {
        assertThatThrownBy(() -> getCompressor().decompress(getCompressedFile(), nonWritableDir))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void decompress_withNullDestinationDirectory_throwsException() {
        assertThatThrownBy(() -> getCompressor().decompress(getCompressedFile(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Destination is null");
    }

    @Test
    public void decompress_nonExistingFile_throwsException() {
        assertThatThrownBy(() -> getCompressor().decompress(NON_EXISTING_FILE, decompressDestinationFile))
                .isInstanceOf(FileNotFoundException.class);
    }

    private void assertCompressionWasSuccessful() throws Exception {
        getCompressor().decompress(compressDestinationFile, decompressDestinationFile);
        assertDecompressionWasSuccessful();
    }

    private void assertDecompressionWasSuccessful() {
        assertThat(decompressDestinationFile).exists().hasSameTextualContentAs(COMPRESS_TXT);
    }
}
