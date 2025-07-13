package io.github.compress4j.compressors;

import io.github.compress4j.compressors.bzip2.BZip2Compressor;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;

class BZip2CompressorTest {

    @TempDir
    private Path tempDir;



    @Test
    void whenGivenPathShouldCompress() throws IOException {
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
