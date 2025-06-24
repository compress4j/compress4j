package io.github.compress4j.compressors;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DecompressorTest {
    @Test
    void inputStreamSameInClassAndBuilderClass() throws IOException {
        var inputStream = mock(CompressorInputStream.class);

        Decompressor.DecompressorBuilder decompressorBuilder = new Decompressor.DecompressorBuilder(inputStream) {
            @Override
            protected Decompressor.DecompressorBuilder getThis() {
                return this;
            }

            @Override
            public Decompressor build() throws IOException {
                return null;
            }
        };

        assertTrue(inputStream == decompressorBuilder.compressorInputStream);
    }

}
