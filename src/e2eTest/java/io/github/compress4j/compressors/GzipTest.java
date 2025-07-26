/*
 * Copyright 2025 The Compress4J Project
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
package io.github.compress4j.compressors;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;
import static org.junit.jupiter.api.Assertions.assertEquals; // todo change to assertj

import io.github.compress4j.compressors.gzip.GzipCompressor;
import io.github.compress4j.compressors.gzip.GzipDecompressor;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GzipTest {

    @TempDir
    Path tempDir;

    private Path sourcePath;

    @BeforeEach
    void setUp() throws IOException {
        sourcePath = createFile(tempDir, "sourceFile", "compressMe");
    }

    @Test
    void compressThenDecompressSameFile() throws Exception {
        var compressPath = tempDir.resolve("compressTest.txt.gz");
        var decompressPath = tempDir.resolve("decompressedTest.txt");

        try (GzipCompressor gzipCompressor =
                GzipCompressor.builder(compressPath).build()) {
            gzipCompressor.write(sourcePath);
        }

        assertThat(compressPath).exists();

        try (GzipDecompressor gZipDecompressor =
                GzipDecompressor.builder(compressPath).build()) {
            gZipDecompressor.write(decompressPath);
        }

        assertThat(decompressPath).exists();

        assertEquals(
                FileUtils.readFileToString(sourcePath.toFile(), "UTF-8"),
                FileUtils.readFileToString(decompressPath.toFile(), "UTF-8"));
    }

    @Test
    void whenCompressingDataWithParamsThenDecompressed() throws Exception {
        var compressPath = tempDir.resolve("compressTest.txt.gz");
        var decompressPath = tempDir.resolve("decompressedTest.txt");

        try (GzipCompressor gzipCompressor = GzipCompressor.builder(compressPath)
                .compressorOutputStreamBuilder()
                .bufferSize(3)
                .compressionLevel(1)
                .parentBuilder()
                .build()) {
            gzipCompressor.write(sourcePath);
        }

        assertThat(compressPath).exists();


        try (GzipDecompressor gZipDecompressor =
                GzipDecompressor.builder(compressPath).build()) {
            gZipDecompressor.write(decompressPath);
        }

        assertThat(decompressPath).exists();
        assertEquals(
                FileUtils.readFileToString(sourcePath.toFile(), "UTF-8"),
                FileUtils.readFileToString(decompressPath.toFile(), "UTF-8"));
    }
}
