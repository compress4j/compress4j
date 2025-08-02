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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

public abstract class AbstractDecompressorTest {

    @TempDir
    protected Path tempDir;

    protected abstract Decompressor<?> decompressorBuilder(Path sourceFile) throws IOException;

    protected abstract void apacheCompressor(Path sourceFile, Path compressedFilePath) throws IOException;

    @Test
    public void whenGivenPathShouldDecompress() throws IOException {
        var sourceFile = createFile(tempDir, "sourceFile", "content");
        var compressed = tempDir.resolve("compressed");
        var targetFile = tempDir.resolve("targetFile");

        apacheCompressor(sourceFile, compressed);

        try (Decompressor<?> decompressor = decompressorBuilder(compressed)) {
            decompressor.write(targetFile);
        }

        assertThat(sourceFile).hasSameBinaryContentAs(targetFile);
    }

    @Test
    void whenGivenEmptyCompressedFileShouldDecompress() throws IOException {
        var originalUncompressedFile = createFile(tempDir, "empty_original.txt", "");
        var compressedSourceFile = tempDir.resolve("empty_test_source.compressed");
        apacheCompressor(originalUncompressedFile, compressedSourceFile);

        var actualDecompressedFile = tempDir.resolve("empty_decompressedActual.txt");

        try (Decompressor<?> decompressor = decompressorBuilder(compressedSourceFile)) {
            decompressor.write(actualDecompressedFile);
        }
        assertThat(actualDecompressedFile).hasSameBinaryContentAs(originalUncompressedFile);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileNonReadableShouldThrowException() throws IOException {
        var originalUncompressedFile = createFile(tempDir, "dummy_original_non_readable_source.txt", "dummy content");
        Path compressedSourceFile = tempDir.resolve("non_readable_source.compressed");
        apacheCompressor(originalUncompressedFile, compressedSourceFile);

        File nonReadableCompressedSourceFile = compressedSourceFile.toFile();
        boolean setReadable = nonReadableCompressedSourceFile.setReadable(false, false); // false for ownerOnly
        Assertions.assertThat(setReadable).isTrue();

        //noinspection resource
        assertThatThrownBy(() -> decompressorBuilder(compressedSourceFile)).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonReadableDirShouldThrowException() throws IOException {
        var originalUncompressedFile = createFile(tempDir, "dummy_original_target_non_readable.txt", "dummy content");
        Path compressedSourceFile = tempDir.resolve("source_for_non_readable_target.compressed");
        apacheCompressor(originalUncompressedFile, compressedSourceFile);

        Path nonReadableTargetFile = createFile(tempDir, "nonReadableTarget.txt", "existing content");
        File nonReadableTargetFileAsFile = nonReadableTargetFile.toFile();
        boolean setReadable = nonReadableTargetFileAsFile.setReadable(false, false);
        Assertions.assertThat(setReadable).isTrue();

        try (Decompressor<?> decompressor = decompressorBuilder(compressedSourceFile)) {
            assertThatThrownBy(() -> decompressor.write(nonReadableTargetFile.toFile()))
                    .isInstanceOf(IOException.class);
        }
    }
}
