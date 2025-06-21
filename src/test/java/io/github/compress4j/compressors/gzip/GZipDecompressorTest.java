package io.github.compress4j.compressors.gzip;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GZipDecompressorTest {
    @Test
    void shouldWritePathEntry() throws Exception {
        //given
        var inputStream = mock(GzipCompressorInputStream.class);

        Path path = mock(Path.class);

        //when

        var aIn = GZipDecompressor.builder(inputStream);
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class);
             GZipDecompressor compressor = new GZipDecompressor(aIn)) {
            compressor.write(path);

            // then
            mockFiles.verify(() -> Files.copy(any(GzipCompressorInputStream.class),any(Path.class)));
        }
    }
}