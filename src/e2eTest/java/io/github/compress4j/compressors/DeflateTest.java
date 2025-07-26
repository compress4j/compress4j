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

import io.github.compress4j.compressors.deflate.DeflateCompressor;
import io.github.compress4j.compressors.deflate.DeflateDecompressor;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeflateTest {

    @TempDir
    Path tempDir;

    Path sourcePath;

    @BeforeEach
    void setUp() throws IOException {
        sourcePath = createFile(tempDir, "sourcePath", "compressMe");
    }

    @Test
    void compressDecompressSameFile() throws Exception {
        var compressPath = tempDir.resolve("compressTest.txt.bz");
        var decompressPath = tempDir.resolve("decompressedTest.txt");

        try (DeflateCompressor deflateCompressor =
                DeflateCompressor.builder(compressPath).build()) {
            deflateCompressor.write(sourcePath);
        }

        assertThat(compressPath).exists();

        try (DeflateDecompressor deflateDecompressor =
                DeflateDecompressor.builder(compressPath).build()) {
            deflateDecompressor.write(decompressPath);
        }

        assertThat(decompressPath).exists();
        Assertions.assertThat(FileUtils.readFileToString(sourcePath.toFile(), "UTF-8"))
                .isEqualTo(FileUtils.readFileToString(decompressPath.toFile(), "UTF-8"));
    }
}
