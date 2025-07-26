package io.github.compress4j.compressors;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.compress4j.assertion.Compress4JAssertions.assertThat;
import static io.github.compress4j.test.util.io.TestFileUtils.createFile;

public abstract class AbstractTest {

    @TempDir
    protected Path tempDir;

    protected Path sourcePath;

    protected abstract Compressor<?> compressorBuilder(Path compressPath) throws IOException;

    protected abstract Decompressor<?> decompressorBuilder(Path compressPath) throws IOException;

    @BeforeEach
    void setUp() throws IOException {
        sourcePath = createFile(tempDir, "sourceFile", "compressMe");
    }
    @Test
    void compressDecompressSameFile() throws Exception {
        var compressPath = tempDir.resolve("compressTest.txt.bz");
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
