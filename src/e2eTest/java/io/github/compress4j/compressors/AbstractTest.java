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

import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public abstract class AbstractTest {

    @TempDir
    protected Path tempDir;

    protected Path sourcePath;

    protected abstract Compressor<?> compressorBuilder(Path compressPath) throws IOException;

    protected abstract Decompressor<?> decompressorBuilder(Path compressPath) throws IOException;

    protected abstract String compressionExtension();

    @BeforeEach
    void setUp() throws IOException {
        sourcePath = createFile(tempDir, "sourceFile", "compressMe");
    }

    @Test
    void compressDecompressSameFile() throws Exception {
        var compressPath = tempDir.resolve("compressTest.txt" + compressionExtension());
        var decompressPath = tempDir.resolve("decompressedTest.txt");

        try (Compressor<?> compressor = compressorBuilder(compressPath)) {
            compressor.write(sourcePath);
        }

        assertThat(compressPath).exists();

        try (Decompressor<?> decompressor = decompressorBuilder(compressPath)) {
            decompressor.write(decompressPath);
        }

        assertThat(decompressPath).exists();
        Assertions.assertThat(FileUtils.readFileToString(sourcePath.toFile(), "UTF-8"))
                .isEqualTo(FileUtils.readFileToString(decompressPath.toFile(), "UTF-8"));
    }
}
