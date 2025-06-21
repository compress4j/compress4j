package io.github.compress4j.compressors.gzip;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class GzipDecompressionBuilderTest {

    @Test
    void builderShouldConstructDecompressorClass() throws IOException {

        var inputStream = mock(GzipCompressorInputStream.class);


        GZipDecompressor.GZipDecompressorBuilder builder = new GZipDecompressor.GZipDecompressorBuilder(inputStream);
        GZipDecompressor actual = builder.build();

        assertTrue(GZipDecompressor.class.equals(actual.getClass()));
    }
}
