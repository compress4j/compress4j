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

//    @Test
//    void compressFileWithBZip2() throws Exception {
//        Path sourceFile = Paths.get("/home/renas/workspace/compress4j/src/test/resources/compressionE2E/compressTest.txt");
//        Path target = tempDir.resolve("compressTest.txt.bz2");
//
//        BZip2Compressor bZip2Compressor = BZip2Compressor.builder(target).build();
//        bZip2Compressor.write(sourceFile);
//
//        assertThat(target).exists();
//
//        Path decompressedFileTarget = Files.createTempFile("compressTest", ".txt");
//
//
//        BZip2Decompressor bZip2Decompressor = new BZip2Decompressor
//                .BZip2DecompressorBuilder(decompressedFileTarget)
//                .build();
//        bZip2Decompressor.write(decompressedFileTarget);
//
//        assertThat(decompressedFileTarget).exists();
//        assertEquals(
//                FileUtils.readFileToString(sourceFile.toFile(), "utf-8"),
//                FileUtils.readFileToString(decompressedFileTarget.toFile(), "utf-8"));
//
//    }



    @Test
    void compressAndDecompressBzip2FileCheckIfsame() throws Exception {
        Path sourceFile = Paths.get("src/test/resources/compressionE2E/compressTest.txt");
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
}
