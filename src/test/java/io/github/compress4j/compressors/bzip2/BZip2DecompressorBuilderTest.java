package io.github.compress4j.compressors.bzip2;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class BZip2DecompressorBuilderTest {

    @Test
    void builderShouldConstructDecompressorClass() throws IOException {

        var inputStream = mock(BZip2CompressorInputStream.class);

        BZip2Decompressor expected = new BZip2Decompressor(inputStream);

        BZip2Decompressor.BZip2DecompressorBuilder builder = new BZip2Decompressor.BZip2DecompressorBuilder(inputStream);
        BZip2Decompressor actual = builder.build();

        assertTrue(expected.getClass() == actual.getClass());
    }
}
