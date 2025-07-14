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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

public abstract class AbstractCompressorTest {

    @TempDir
    private Path tempDir;

    protected abstract Compressor compressorBuilder(Path targetPath) throws IOException;

    protected abstract void appacheCompressor(Path sourceFile, Path expectedPath) throws IOException;

    @Test
    public void whenGivenPathShouldCompress() throws IOException {
        // when
        var sourceFilePath = createFile(tempDir, "source.txt", "Lorem impsum");
        var targetFilePath = tempDir.resolve("compressedActual.txt");
        var expectedFilePath = tempDir.resolve("compressedExpected.txt");
        appacheCompressor(sourceFilePath, expectedFilePath);

        try (Compressor compressor = compressorBuilder(targetFilePath)) {
            compressor.write(sourceFilePath);
        }

        assertThatCompressionIsSuccessfull(targetFilePath, sourceFilePath, expectedFilePath);
    }

    @Test
    void whenGivenEmptyPathShouldCompress() throws IOException {
        var emptySourceFile = createFile(tempDir, "empty.txt", ""); // Create an empty file
        var targetFilePath = tempDir.resolve("compressedActual.txt");
        var expectedFilePath = tempDir.resolve("compressedExpected.txt");
        appacheCompressor(emptySourceFile, expectedFilePath);

        try (Compressor compressor = compressorBuilder(targetFilePath)) {
            compressor.write(emptySourceFile);
        }

        assertThatCompressionIsSuccessfull(targetFilePath, emptySourceFile, expectedFilePath);
    }

    @Test
    void whenSourceFileNonReadableShouldThrowExepction() throws IOException {
        File nonReadableSourceFile =
                createFile(tempDir, "sourceFile.txt", "nonReadable").toFile();
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        nonReadableSourceFile.setReadable(
                false); // todo all of these are ignored -> do not understand how the test is still passing

        assertThatIOexpectionIsThrown(targetFilePath, nonReadableSourceFile);
    }

    @Test
    void whenSourceFileNonWritableShouldThrowExepction() {
        File nonWritableSourceFile = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        nonWritableSourceFile.setWritable(false);

        assertThatIOexpectionIsThrown(targetFilePath, nonWritableSourceFile);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonReadableDirShouldThrowExepction() {
        File nonReadableDir = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        nonReadableDir.mkdir();
        nonReadableDir.setReadable(false);

        assertThatIOexpectionIsThrown(targetFilePath, nonReadableDir);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonWritableDirShouldThrowExepction() {
        File nonWritableDir = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        nonWritableDir.mkdir();
        nonWritableDir.setWritable(false);

        assertThatIOexpectionIsThrown(targetFilePath, nonWritableDir);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenTargetPathNonReadableShouldThrowExepction() {
        File sourceFile = new File(tempDir.toFile(), "source.txt");
        File nonReadableTargetFile = new File(tempDir.toFile(), "target.txt");

        nonReadableTargetFile.setReadable(false);
        Path nonReadableTargetPath = nonReadableTargetFile.toPath();

        assertThatIOexpectionIsThrown(nonReadableTargetPath, sourceFile);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenTargetPathNonWritableShouldThrowExepction() {
        File sourceFile = new File(tempDir.toFile(), "source.txt");
        File nonWritableSourceFile = new File(tempDir.toFile(), "target.txt");

        nonWritableSourceFile.setWritable(false);
        Path nonWritableTargetPath = nonWritableSourceFile.toPath();

        assertThatIOexpectionIsThrown(nonWritableTargetPath, sourceFile);
    }

    @Test
    void whenTargetPathIsNonExistent() {
        File sourceFile = new File(tempDir.toFile(), "source.txt");
        Path targetFilePath = Path.of("non/existent/file/path.txt");

        assertThatIOexpectionIsThrown(targetFilePath, sourceFile);
    }

    @Test
    void whenSourcePathIsNonExistent() {
        Path sourceFile = Path.of("non/existent/file/path.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        assertThatIOexpectionIsThrown(targetFilePath, sourceFile.toFile());
    }

    private static void assertThatCompressionIsSuccessfull(
            Path targetFilePath, Path sourceFilePath, Path expectedFilePath) {
        assertThat(targetFilePath).exists();
        assertThat(sourceFilePath).exists();
        assertThat(targetFilePath).hasSameBinaryContentAs(expectedFilePath);
    }

    private void assertThatIOexpectionIsThrown(Path targetPath, File sourcePath) {
        assertThatThrownBy(() -> {
                    Compressor compressor = compressorBuilder(targetPath);
                    compressor.write(sourcePath);
                })
                .isInstanceOf(IOException.class);
    }
}
