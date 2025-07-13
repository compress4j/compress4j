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
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;

public class BZip2CompressorTest {

    @TempDir
    private Path targetTempDir;

    @TempDir
    private Path expectedFilePath;

    private Path sourceFilePath;
    private Path targetFilePath;

    @BeforeEach
    void setUp() throws IOException {
        sourceFilePath = Paths.get("/home/renas/workspace/compress4j/src/integrationTest/resources/compression/compressTest.txt");
        targetFilePath = targetTempDir.resolve("compressedActual.txt.bz2");
        expectedFilePath = expectedFilePath.resolve("compressedExpected.txt.bz2");

    }

    @Test
    void whenGivenPathShouldCompress() throws IOException {
        //when
        try(BZip2Compressor bZip2Compressor = new BZip2Compressor.BZip2CompressorBuilder(targetFilePath).build()) {
            bZip2Compressor.write(sourceFilePath);
        }

        //given
        try (InputStream in = new FileInputStream(sourceFilePath.toFile());
             OutputStream out = new FileOutputStream(expectedFilePath.toFile());
             BZip2CompressorOutputStream bzipOut = new BZip2CompressorOutputStream(out)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                bzipOut.write(buffer, 0, bytesRead);
            }
        }


        assertThat(targetFilePath).exists();
        assertThat(sourceFilePath).exists();

        assertThat(targetFilePath).hasSameBinaryContentAs(expectedFilePath);
    }

    @Test
    void whenGivenOutputStreamShouldCompress(){}

    @Test
    void whenGivenEmptyFileShouldCompress(){}

    @Test
    void whenGivenEmptyPathShouldCompress(){}

    @Test
    void whenGiven_Empty_OutputStreamShould_not_Compress_QUESTION(){}

    @Test
    void whenGivenInvalidPathShouldReturnExpection(){}

    @Test
    void whenGivenInvalidFileShouldReturnExpection(){}

    @Test
    void whenGivenDifferentBlockSizeValuesShouldCompress(){}
}
