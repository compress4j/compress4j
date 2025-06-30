package compressors;

import io.github.compress4j.compressors.Decompressor;
import io.github.compress4j.compressors.bzip2.BZip2Compressor;
import io.github.compress4j.compressors.bzip2.BZip2Decompressor;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BZip2compressionE2E {

    @TempDir
    Path tempDir;

    @Test
    void compressDecompressSameFile() throws Exception {
        Path sourceFile = Paths.get("/home/renas/workspace/compress4j/src/e2e/resources/compression/compressTest.txt");
        Path compressedTarget = tempDir.resolve("compressTest.txt.bz2");

        try (BZip2Compressor bZip2Compressor = BZip2Compressor.builder(compressedTarget).build()) {
            bZip2Compressor.write(sourceFile);
        }

        assertThat(compressedTarget).exists();

        Path decompressedFileTarget = tempDir.resolve("decompressedTest.txt");

        try (BZip2Decompressor bZip2Decompressor = BZip2Decompressor.builder(compressedTarget).build()) {
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

        try (BZip2Compressor bZip2Compressor = BZip2Compressor.builder(compressedTarget).compressorOutputStreamBuilder().blockSize(6).parentBuilder().build()) {
            bZip2Compressor.write(sourceFile);
        }

        assertThat(compressedTarget).exists();

        Path decompressedFileTarget = tempDir.resolve("decompressedTest.txt");

        try (BZip2Decompressor bZip2Decompressor = BZip2Decompressor.builder(compressedTarget).build()) {
            bZip2Decompressor.write(decompressedFileTarget);
        }

        assertThat(decompressedFileTarget).exists();
        assertEquals(
                FileUtils.readFileToString(sourceFile.toFile(), "UTF-8"),
                FileUtils.readFileToString(decompressedFileTarget.toFile(), "UTF-8"));
    }

}
