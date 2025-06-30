package compressors;

import io.github.compress4j.compressors.gzip.GZipDecompressor;
import io.github.compress4j.compressors.gzip.GzipCompressor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GzipCompressionE2E {

    @TempDir
    Path tempDir;

    @Test
    void whenCompressingDataThenDecompressed() throws Exception {
        Path sourceFile = Paths.get("/home/renas/workspace/compress4j/src/e2e/resources/compression/compressTest.txt");
        Path compressedTarget = tempDir.resolve("compressTest.txt.bz2");

        try (GzipCompressor gzipCompressor = GzipCompressor.builder(compressedTarget).build()) {
            gzipCompressor.write(sourceFile);
        }

        assertThat(compressedTarget).exists();

        Path decompressedFileTarget = tempDir.resolve("decompressedTest.txt");


        try(GZipDecompressor gZipDecompressor = GZipDecompressor.builder(compressedTarget).build()) {
            gZipDecompressor.write(decompressedFileTarget);
        }

        assertThat(decompressedFileTarget).exists();
        assertEquals(
                FileUtils.readFileToString(sourceFile.toFile(), "UTF-8"),
                FileUtils.readFileToString(decompressedFileTarget.toFile(), "UTF-8"));
    }

    @Test
    void whenCompressingDataWithParamsThenDecompressed() throws Exception {
        Path sourceFile = Paths.get("/home/renas/workspace/compress4j/src/e2e/resources/compression/compressTest.txt");
        Path compressedTarget = tempDir.resolve("compressTest.txt.bz2");

        try (GzipCompressor gzipCompressor = GzipCompressor.builder(compressedTarget).compressorOutputStreamBuilder()
                .bufferSize(3)
                .compressionLevel(1)
                .parentBuilder()
                .build()) {
            gzipCompressor.write(sourceFile);
        }

        assertThat(compressedTarget).exists();

        Path decompressedFileTarget = tempDir.resolve("decompressedTest.txt");


        try(GZipDecompressor gZipDecompressor = GZipDecompressor.builder(compressedTarget).build()) {
            gZipDecompressor.write(decompressedFileTarget);
        }

        assertThat(decompressedFileTarget).exists();
        assertEquals(
                FileUtils.readFileToString(sourceFile.toFile(), "UTF-8"),
                FileUtils.readFileToString(decompressedFileTarget.toFile(), "UTF-8"));
    }

}
