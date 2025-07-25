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
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

public abstract class AbstractCompressorTest {

    @TempDir
    private Path tempDir;

    protected abstract Compressor<?> compressorBuilder(Path targetPath) throws IOException;

    protected abstract void apacheCompressor(Path sourceFile, Path expectedPath) throws IOException;

    @Test
    public void whenGivenPathShouldCompress() throws IOException {
        var sourceFilePath = createFile(tempDir, "source.txt", "Lorem impsum");
        var targetFilePath = tempDir.resolve("compressedActual.txt");
        var expectedFilePath = tempDir.resolve("compressedExpected.txt");
        apacheCompressor(sourceFilePath, expectedFilePath);

        try (Compressor<?> compressor = compressorBuilder(targetFilePath)) {
            compressor.write(sourceFilePath);
        }

        assertThatCompressionIsSuccessful(targetFilePath, sourceFilePath, expectedFilePath);
    }

    @Test
    void whenGivenEmptyPathShouldCompress() throws IOException {
        var emptySourceFile = createFile(tempDir, "empty.txt", ""); // Create an empty file
        var targetFilePath = tempDir.resolve("compressedActual.txt");
        var expectedFilePath = tempDir.resolve("compressedExpected.txt");
        apacheCompressor(emptySourceFile, expectedFilePath);

        try (Compressor<?> compressor = compressorBuilder(targetFilePath)) {
            compressor.write(emptySourceFile);
        }

        assertThatCompressionIsSuccessful(targetFilePath, emptySourceFile, expectedFilePath);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileNonReadableShouldThrowException() throws IOException {
        File nonReadableSourceFile =
                createFile(tempDir, "sourceFile.txt", "nonReadable").toFile();
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        boolean setReadable = nonReadableSourceFile.setReadable(false, false); // false for ownerOnly

        org.assertj.core.api.Assertions.assertThat(setReadable).isTrue();
        assertThatIOExceptionIsThrown(targetFilePath, nonReadableSourceFile);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonReadableDirShouldThrowException() {
        File nonReadableDir = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        boolean makeDirectory = nonReadableDir.mkdir();
        boolean nonReadable = nonReadableDir.setReadable(false);

        org.assertj.core.api.Assertions.assertThat(makeDirectory).isTrue();
        org.assertj.core.api.Assertions.assertThat(nonReadable).isTrue();
        assertThatIOExceptionIsThrown(targetFilePath, nonReadableDir);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonWritableDirShouldThrowException() {
        File nonWritableDir = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        boolean makeDirectory = nonWritableDir.mkdir();
        boolean nonWritable = nonWritableDir.setWritable(false);

        org.assertj.core.api.Assertions.assertThat(makeDirectory).isTrue();
        org.assertj.core.api.Assertions.assertThat(nonWritable).isTrue();
        assertThatIOExceptionIsThrown(targetFilePath, nonWritableDir);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenTargetPathNonReadableShouldThrowException() throws IOException {
        File sourceFile = new File(tempDir.toFile(), "source.txt");
        File nonReadableTargetFile =
                createFile(tempDir, "TargetFile.txt", "nonReadable").toFile();

        boolean nonReadable = nonReadableTargetFile.setReadable(false);
        org.assertj.core.api.Assertions.assertThat(nonReadable).isTrue();

        Path nonReadableTargetPath = nonReadableTargetFile.toPath();

        assertThatIOExceptionIsThrown(nonReadableTargetPath, sourceFile);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenTargetPathNonWritableShouldThrowException() throws IOException {
        File nonWritableSourceFile =
                createFile(tempDir, "TargetFile.txt", "nonReadable").toFile();

        boolean nonWritable = nonWritableSourceFile.setWritable(false, false);
        org.assertj.core.api.Assertions.assertThat(nonWritable).isTrue();

        Path nonWritableTargetPath = nonWritableSourceFile.toPath();

        //noinspection resource
        assertThatThrownBy(() -> compressorBuilder(nonWritableTargetPath)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void whenTargetPathIsNonExistent() {
        Path targetFilePath = Path.of("non/existent/file/path.txt");

        //noinspection resource
        assertThatThrownBy(() -> compressorBuilder(targetFilePath)).isInstanceOf(NoSuchFileException.class);
    }

    @Test
    void whenSourcePathIsNonExistent() {
        Path sourceFile = Path.of("non/existent/file/path.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt");

        assertThatIOExceptionIsThrown(targetFilePath, sourceFile.toFile());
    }

    private static void assertThatCompressionIsSuccessful(
            Path targetFilePath, Path sourceFilePath, Path expectedFilePath) {
        assertThat(targetFilePath).exists();
        assertThat(sourceFilePath).exists();
        assertThat(targetFilePath).hasSameBinaryContentAs(expectedFilePath);
    }

    private void assertThatIOExceptionIsThrown(Path targetPath, File sourcePath) {
        try {
            //noinspection resource
            Compressor<?> compressor = compressorBuilder(targetPath);
            assertThatThrownBy(() -> compressor.write(sourcePath)).isInstanceOf(IOException.class);
        } catch (IOException e) {
            fail("Failed to create compressor for path: " + targetPath, e);
        }
    }
}
