package io.github.compress4j.compressors;

import io.github.compress4j.compressors.bzip2.BZip2Compressor;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BZip2CompressorTest{

    @TempDir
    private Path tempDir;

    @Test
    void whenGivenPathShouldCompress() throws IOException{
        //when
        var sourceFilePath = createFile(tempDir, "source.txt", "Lorem impsum");
        var targetFilePath = tempDir.resolve("compressedActual.txt.bz2");
        var expectedFilePath = tempDir.resolve("compressedExpected.txt.bz2");
        appacheCompressor(sourceFilePath, expectedFilePath);

        try(BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build()) {
            bZip2Compressor.write(sourceFilePath);
        }

        //should
        assertThat(targetFilePath).exists();
        assertThat(sourceFilePath).exists();

        assertThat(targetFilePath).hasSameBinaryContentAs(expectedFilePath);
    }

    @Test
    void whenGivenOutputStreamShouldCompress() throws IOException {
        //when
        var sourceFilePath = createFile(tempDir, "source.txt", "Lorem impsum");
        var targetFilePath = tempDir.resolve("compressedActual.txt.bz2");
        var expectedFilePath = tempDir.resolve("compressedExpected.txt.bz2");
        appacheCompressor(sourceFilePath, expectedFilePath);

        OutputStream targetOutputStream = Files.newOutputStream(targetFilePath);

        try(BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetOutputStream).build()) {
            bZip2Compressor.write(sourceFilePath);
        }

        //should
        assertThat(targetFilePath).exists();
        assertThat(sourceFilePath).exists();

        assertThat(targetFilePath).hasSameBinaryContentAs(expectedFilePath);
    }

    @Test
    void whenGivenEmptyPathShouldCompress() throws IOException{
        var emptySourceFile = createFile(tempDir, "empty.txt", ""); // Create an empty file
        var targetFilePath = tempDir.resolve("compressedActual.txt.bz2");
        var expectedFilePath =  tempDir.resolve("decompressed_empty.txt");


        try(BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build()) {
            bZip2Compressor.write(emptySourceFile);
        }

        appacheCompressor(emptySourceFile, expectedFilePath);
        assertThat(targetFilePath).exists();
        assertThat(emptySourceFile).exists();

        assertThat(targetFilePath).hasSameBinaryContentAs(expectedFilePath);
    }

    @Test
    void whenSourceFileNonReadableShouldThrowExepction() throws IOException {
        File nonReadableSourceFile = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt.bz2");

        nonReadableSourceFile.setReadable(false);

        assertThatThrownBy(() -> {
            BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build();
            bZip2Compressor.write(nonReadableSourceFile);
        }).isInstanceOf(IOException.class);
    }


    @Test
    void whenSourceFileNonWritableShouldThrowExepction() throws IOException {
        File nonWritableSourceFile = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt.bz2");

        nonWritableSourceFile.setWritable(false);

        assertThatThrownBy(() -> {
            BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build();
            bZip2Compressor.write(nonWritableSourceFile);
        }).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonReadableDirShouldThrowExepction() throws IOException {
        File nonReadableDir = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt.bz2");

        nonReadableDir.mkdir();
        nonReadableDir.setReadable(false);

        assertThatThrownBy(() -> {
            BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build();
            bZip2Compressor.write(nonReadableDir);
        }).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenSourceFileInNonWritableDirShouldThrowExepction() throws IOException {
        File nonWritableDir = new File(tempDir.toFile(), "source.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt.bz2");

        nonWritableDir.mkdir();
        nonWritableDir.setWritable(false);

        assertThatThrownBy(() -> {
            BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build();
            bZip2Compressor.write(nonWritableDir);
        }).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenTargetPathNonReadableShouldThrowExepction() throws IOException {
        File SourceFile = new File(tempDir.toFile(), "source.txt");
        File nonReadableTargetFile = new File(tempDir.toFile(), "target.txt");

        nonReadableTargetFile.setReadable(false);
        Path nonReadableTargetPath = nonReadableTargetFile.toPath();
        assertThatThrownBy(() -> {
            BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(nonReadableTargetPath).build();
            bZip2Compressor.write(SourceFile);
        }).isInstanceOf(IOException.class);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void whenTargetPathNonWritableShouldThrowExepction() throws IOException {
        File SourceFile = new File(tempDir.toFile(), "source.txt");
        File nonWritableSourceFile = new File(tempDir.toFile(), "target.txt");

        nonWritableSourceFile.setWritable(false);
        Path nonWritableTargetPath = nonWritableSourceFile.toPath();

        assertThatThrownBy(() -> {
            BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(nonWritableTargetPath).build();
            bZip2Compressor.write(SourceFile);
        }).isInstanceOf(IOException.class);
    }

    @Test
    void whenTargetPathIsNonExistent() throws IOException {
        File sourceFile = new File(tempDir.toFile(), "source.txt");
        Path targetFilePath = Path.of("non/existent/file/path.txt");

        assertThatThrownBy(() -> {
            BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build();
            bZip2Compressor.write(sourceFile);
        }).isInstanceOf(IOException.class);
    }

    @Test
    void whenSourcePathIsNonExistent() throws IOException {
        Path sourceFile = Path.of("non/existent/file/path.txt");
        var targetFilePath = tempDir.resolve("compressedActual.txt.bz2");

        assertThatThrownBy(() -> {
            BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build();
            bZip2Compressor.write(sourceFile);
        }).isInstanceOf(IOException.class);
    }
    private void appacheCompressor(Path sourceFilePath, Path expectedFilePath) throws IOException {
        try (InputStream in = new FileInputStream(sourceFilePath.toFile());
             OutputStream out = new FileOutputStream(expectedFilePath.toFile());
             BZip2CompressorOutputStream bzipOut = new BZip2CompressorOutputStream(out)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                bzipOut.write(buffer, 0, bytesRead);
            }
        }
    }
}
