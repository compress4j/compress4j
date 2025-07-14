package io.github.compress4j.compressors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class AbstractCompressorTest {

    @TempDir
    private Path tempDir;

    protected abstract Compressor compressorBuilder(Path targetPath) throws IOException;

    protected abstract String getCompressorExtension();

    protected abstract void appacheCompressor(Path sourceFile, Path expectedPath) throws IOException;

    @Test
    public void whenGivenPathShouldCompress() throws IOException {
        //when
        var sourceFilePath = createFile(tempDir, "source.txt", "Lorem impsum");
        var targetFilePath = tempDir.resolve("compressedActual.txt" + getCompressorExtension());
        var expectedFilePath = tempDir.resolve("compressedExpected.txt" + getCompressorExtension());
        appacheCompressor(sourceFilePath, expectedFilePath);

        try (Compressor compressor = compressorBuilder(targetFilePath)) {
            compressor.write(sourceFilePath);
        }
        //should
        assertThat(targetFilePath).exists();
        assertThat(sourceFilePath).exists();

        assertThat(targetFilePath).hasSameBinaryContentAs(expectedFilePath);
    }

    @Test
    void whenGivenEmptyPathShouldCompress() throws IOException {
        var emptySourceFile = createFile(tempDir, "empty.txt", ""); // Create an empty file
        var targetFilePath = tempDir.resolve("compressedActual.txt" + getCompressorExtension());
        var expectedFilePath = tempDir.resolve("compressedExpected.txt" + getCompressorExtension());
        appacheCompressor(emptySourceFile, expectedFilePath);

        try (Compressor compressor = compressorBuilder(targetFilePath)) {
            compressor.write(emptySourceFile);
        }

        assertThat(targetFilePath).exists();
        assertThat(emptySourceFile).exists();
        assertThat(targetFilePath).hasSameBinaryContentAs(expectedFilePath);
    }

    @Test
    void whenSourceFileNonReadableShouldThrowExepction(){
        File nonReadableSourceFile = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt" + getCompressorExtension());

        nonReadableSourceFile.setReadable(false);

        assertThatThrownBy(() -> {
            Compressor compressor = compressorBuilder(targetFilePath);
            compressor.write(nonReadableSourceFile);

        }).isInstanceOf(IOException.class);
    }


    @Test
    void whenSourceFileNonWritableShouldThrowExepction(){
        File nonWritableSourceFile = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt" + getCompressorExtension());

        nonWritableSourceFile.setWritable(false);

        assertThatThrownBy(() -> {
            Compressor compressor = compressorBuilder(targetFilePath);
            compressor.write(nonWritableSourceFile);
        }).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonReadableDirShouldThrowExepction(){
        File nonReadableDir = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt" + getCompressorExtension());

        nonReadableDir.mkdir();
        nonReadableDir.setReadable(false);

        assertThatThrownBy(() -> {
            Compressor compressor = compressorBuilder(targetFilePath);
            compressor.write(nonReadableDir);
        }).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonWritableDirShouldThrowExepction(){
        File nonWritableDir = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt" + getCompressorExtension());

        nonWritableDir.mkdir();
        nonWritableDir.setWritable(false);

        assertThatThrownBy(() -> {
            Compressor compressor = compressorBuilder(targetFilePath);
            compressor.write(nonWritableDir);
        }).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenTargetPathNonReadableShouldThrowExepction(){
        File sourceFile = new File(tempDir.toFile(), "source.txt");
        File nonReadableTargetFile = new File(tempDir.toFile(), "target.txt");

        nonReadableTargetFile.setReadable(false);
        Path nonReadableTargetPath = nonReadableTargetFile.toPath();

        assertThatThrownBy(() -> {
            Compressor compressor = compressorBuilder(nonReadableTargetPath);
            compressor.write(sourceFile);
        }).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenTargetPathNonWritableShouldThrowExepction(){
        File sourceFile = new File(tempDir.toFile(), "source.txt");
        File nonWritableSourceFile = new File(tempDir.toFile(), "target.txt");

        nonWritableSourceFile.setWritable(false);
        Path nonWritableTargetPath = nonWritableSourceFile.toPath();

        assertThatThrownBy(() -> {
            Compressor compressor = compressorBuilder(nonWritableTargetPath);
            compressor.write(sourceFile);
        }).isInstanceOf(IOException.class);
    }

    @Test
    void whenTargetPathIsNonExistent(){
        File sourceFile = new File(tempDir.toFile(), "source.txt");
        Path targetFilePath = Path.of("non/existent/file/path.txt");

        assertThatThrownBy(() -> {
            Compressor compressor = compressorBuilder(targetFilePath);
            compressor.write(sourceFile);
        }).isInstanceOf(IOException.class);
    }

    @Test
    void whenSourcePathIsNonExistent(){
        Path sourceFile = Path.of("non/existent/file/path.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt" + getCompressorExtension());

        assertThatThrownBy(() -> {
            Compressor compressor = compressorBuilder(targetFilePath);
            compressor.write(sourceFile);
        }).isInstanceOf(IOException.class);
    }

}
