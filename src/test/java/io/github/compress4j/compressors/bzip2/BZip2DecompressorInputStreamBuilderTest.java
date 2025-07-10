package io.github.compress4j.compressors.bzip2;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import io.github.compress4j.compressors.bzip2.BZip2Decompressor.BZip2DecompressorBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class BZip2DecompressorInputStreamBuilderTest {

    @Mock
    private InputStream mockRawInputStream;

    private BZip2DecompressorBuilder parentBuilder;

    @BeforeEach
    void setUp() {
       parentBuilder = new BZip2DecompressorBuilder(mockRawInputStream);
    }

    @Test
    void shouldBuildInputStream() throws IOException {
        BZip2Decompressor.BZip2DecompressorInputStreamBuilder compressorInputStreamBuilder = spy(new BZip2Decompressor.BZip2DecompressorInputStreamBuilder<>(parentBuilder,mockRawInputStream));

        doReturn(mock(BZip2CompressorInputStream.class)).when(compressorInputStreamBuilder).buildInputStream();

        // when
        try (BZip2CompressorInputStream buildCompresserInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompresserInputStream).isInstanceOf(BZip2CompressorInputStream.class);
        }
    }

    @Test
    void shouldBuildInputStreamWithDecompressConcatTrue() throws IOException, NoSuchFieldException, IllegalAccessException {
        BZip2Decompressor.BZip2DecompressorInputStreamBuilder<BZip2DecompressorBuilder> compressorInputStreamBuilder = spy(parentBuilder.inputStreamBuilder());

        doReturn(mock(BZip2CompressorInputStream.class)).when(compressorInputStreamBuilder).buildInputStream();
        compressorInputStreamBuilder.setDecompressConcatenated(true);

        Field decompressConcatenatedField = BZip2Decompressor.BZip2DecompressorInputStreamBuilder.class.getDeclaredField("decompressConcatenated");
        decompressConcatenatedField.setAccessible(true);

        boolean decompressConcatenatedValue = (boolean) decompressConcatenatedField.get(compressorInputStreamBuilder);

        assertThat(decompressConcatenatedValue).isTrue();


        // when
        try (BZip2CompressorInputStream buildCompresserInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompresserInputStream).isInstanceOf(BZip2CompressorInputStream.class);
        }
    }
}