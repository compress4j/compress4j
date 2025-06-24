package io.github.compress4j.compressors.bzip2;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BZip2DecompressorTest {
    @Test
    void shouldWritePathEntry() throws Exception {
        // given
        var inputStream = mock(InputStream.class);

        final Path tempSourceFile1 = mock(Path.class);

        // when
        var aOut = spy(BZip2Decompressor.builder(inputStream).build());
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class);
             BZip2Decompressor decompressor = new BZip2Decompressor(aOut)) {

            decompressor.write(tempSourceFile1);

            // then
            mockFiles.verify(() -> Files.copy(any(Path.class), any(OutputStream.class)));
        }
    }
}