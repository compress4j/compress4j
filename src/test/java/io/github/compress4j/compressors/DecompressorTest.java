package io.github.compress4j.compressors;

import io.github.compress4j.compressors.memory.InMemoryCompressor;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @Test
    void shouldWriteFileEntry() throws Exception {
        // given
        var outputStream = mock(OutputStream.class);
        var tempSourceFile = mock(File.class);
        var tempSourcePath = mock(Path.class);
        given(tempSourceFile.toPath()).willReturn(tempSourcePath);

        //when
        var aOut = spy(InMemoryCompressor.builder(outputStream).buildCompressorOutputStream());
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class);
             InMemoryCompressor compressor = new InMemoryCompressor(aOut)) {

            compressor.write(tempSourceFile);

            // then
            mockFiles.verify(() -> Files.copy(eq(tempSourcePath), any(OutputStream.class)));
        }
    }

    @Test
    void shouldWritePathEntry() throws Exception {
        // given
        var outputStream = mock(OutputStream.class);
        var tempSourcePath = mock(Path.class);

        // when
        var aOut = spy(InMemoryCompressor.builder(outputStream).buildCompressorOutputStream());
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class);
             InMemoryCompressor compressor = new InMemoryCompressor(aOut)) {

            compressor.write(tempSourcePath);

            // then
            mockFiles.verify(() -> Files.copy(eq(tempSourcePath), any(OutputStream.class)));
        }
    }

}
