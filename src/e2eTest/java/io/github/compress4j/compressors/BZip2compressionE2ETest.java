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
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.compress4j.compressors.bzip2.BZip2Compressor;
import io.github.compress4j.compressors.bzip2.BZip2Decompressor;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Disabled
class BZip2compressionE2ETest {

    @TempDir
    Path tempDir;

    @Test
    void compressDecompressSameFile() throws Exception {
        Path sourceFile = Paths.get("/home/renas/workspace/compress4j/src/e2e/resources/compression/compressTest.txt");
        Path compressedTarget = tempDir.resolve("compressTest.txt.bz2");

        try (BZip2Compressor bZip2Compressor =
                BZip2Compressor.builder(compressedTarget).build()) {
            bZip2Compressor.write(sourceFile);
        }

        assertThat(compressedTarget).exists();

        Path decompressedFileTarget = tempDir.resolve("decompressedTest.txt");

        try (BZip2Decompressor bZip2Decompressor =
                BZip2Decompressor.builder(compressedTarget).build()) {
            bZip2Decompressor.write(decompressedFileTarget);
        }

        assertThat(decompressedFileTarget).exists();
        assertEquals(
                FileUtils.readFileToString(sourceFile.toFile(), "UTF-8"),
                FileUtils.readFileToString(decompressedFileTarget.toFile(), "UTF-8"));
    }

    @Test
    void compressWithParametersThenDecompress() throws Exception {
        Path sourceFile = Paths.get("/home/renas/workspace/compress4j/src/e2e/resources/compression/compressTest.txt");
        Path compressedTarget = tempDir.resolve("compressTest.txt.bz2");

        try (BZip2Compressor bZip2Compressor = BZip2Compressor.builder(compressedTarget)
                .compressorOutputStreamBuilder()
                .blockSize(6)
                .parentBuilder()
                .build()) {
            bZip2Compressor.write(sourceFile);
        }

        assertThat(compressedTarget).exists();

        Path decompressedFileTarget = tempDir.resolve("decompressedTest.txt");

        try (BZip2Decompressor bZip2Decompressor =
                BZip2Decompressor.builder(compressedTarget).build()) {
            bZip2Decompressor.write(decompressedFileTarget);
        }

        assertThat(decompressedFileTarget).exists();
        assertEquals(
                FileUtils.readFileToString(sourceFile.toFile(), "UTF-8"),
                FileUtils.readFileToString(decompressedFileTarget.toFile(), "UTF-8"));
    }
}
