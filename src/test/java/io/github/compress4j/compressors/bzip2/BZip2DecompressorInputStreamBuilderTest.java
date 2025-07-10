package io.github.compress4j.compressors.bzip2;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BZip2DecompressorInputStreamBuilderTest {
    private InputStream mockRawInputStream;

    @BeforeEach
    void setUp() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] emptyValidBZip2Data = bos.toByteArray();
        mockRawInputStream = new ByteArrayInputStream(emptyValidBZip2Data);
    }

    @Test
    void shouldBuildInputStream() throws IOException {
        var builder = BZip2Decompressor.builder(mockRawInputStream);

        // when
        try (BZip2CompressorInputStream in = builder.buildCompressorInputStream()) {
            // then
            assertThat(in).isNotNull();
        }
    }

    @Test
    void shouldBuildInputStreamWithDecompressConcatTrue() throws IOException {
        var builder = BZip2Decompressor.builder(mockRawInputStream);

        // when
        try (BZip2CompressorInputStream in =
                     builder.inputStreamBuilder().setDecompressConcatenated(true).buildInputStream()) {
            // then
            assertThat(in).isNotNull();
        }
    }
}