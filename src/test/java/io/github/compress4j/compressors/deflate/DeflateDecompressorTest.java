/*
 * Copyright 2025 The Compress4J Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.compress4j.compressors.deflate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeflateDecompressorTest {

    @Mock
    private DeflateCompressorInputStream mockedDeflateCompressorInputStream;

    private DeflateDecompressor deflateDecompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        deflateDecompressor = new DeflateDecompressor(mockedDeflateCompressorInputStream);
    }

    @Test
    @DisplayName("Should construct a deflate decompressor using Builder")
    void shouldConstructGzipDecompressorUsingBuilder() throws IOException {

        DeflateDecompressor.DeflateDecompressorBuilder mockBuilder =
                mock(DeflateDecompressor.DeflateDecompressorBuilder.class);

        DeflateDecompressor decompressorFromBuilder = new DeflateDecompressor(mockBuilder);

        assertThat(decompressorFromBuilder).isNotNull();
    }

    @Test
    @DisplayName("Should write all bytes from input stream to a file")
    void write_ToFile_CopiesBytes() throws IOException {
        // given
        Path outputPath = tempDir.resolve("output.txt");
        byte[] testBytes = "deflate is Great".getBytes();

        when(mockedDeflateCompressorInputStream.transferTo(any(OutputStream.class)))
                .thenAnswer(invocation -> {
                    OutputStream outputStream = invocation.getArgument(0);
                    outputStream.write(testBytes);
                    return ((Number) testBytes.length).longValue();
                });

        // when
        long bytesWritten = deflateDecompressor.write(outputPath.toFile());

        // then
        assertThat(outputPath).exists().hasContent("deflate is Great");
        assertThat(bytesWritten).isEqualTo(testBytes.length);
    }

    @Test
    @DisplayName("Should write all bytes from input stream to a path")
    void write_ToPath_CopiesBytes() throws IOException {
        // given
        Path outputPath = tempDir.resolve("output.txt");
        byte[] testBytes = "deflate is Great".getBytes();

        when(mockedDeflateCompressorInputStream.transferTo(any(OutputStream.class)))
                .thenAnswer(invocation -> {
                    OutputStream outputStream = invocation.getArgument(0);
                    outputStream.write(testBytes);
                    return ((Number) testBytes.length).longValue();
                });

        // when
        long bytesWritten = deflateDecompressor.write(outputPath);

        // then
        assertThat(bytesWritten).isEqualTo(testBytes.length);
        assertThat(outputPath).exists().hasContent("deflate is Great");
    }

    @Test
    @DisplayName("Should throw IOException when writing to file fails")
    void write_ToFile_ThrowsIOException_WhenCopyFails() {
        File nonWritableFile = new File("/nonexistent/path/cannot_write.txt");

        assertThatThrownBy(() -> deflateDecompressor.write(nonWritableFile))
                .isInstanceOf(IOException.class)
                .hasMessage(nonWritableFile.toString());
    }

    @Test
    @DisplayName("Should throw IOException when writing to path fails")
    void write_ToPath_ThrowsIOException_WhenCopyFails() {
        Path nonWritablePath = tempDir.resolve("non_existent_dir/output.txt");

        assertThatThrownBy(() -> deflateDecompressor.write(nonWritablePath))
                .isInstanceOf(IOException.class)
                .hasMessage(nonWritablePath.toString());
    }

    @Test
    @DisplayName("Should close the deflate compressor input stream")
    void close_ClosesBZip2CompressorInputStream() throws IOException {
        deflateDecompressor.close();
        verify(mockedDeflateCompressorInputStream, times(1)).close();
    }

    @Test
    @DisplayName("Should throw IOException when closing deflate compressor input stream fails")
    void close_ThrowsIOException_WhenBZip2CompressorInputStreamCloseFails() throws IOException {
        doThrow(new IOException("Failed to close deflate stream"))
                .when(mockedDeflateCompressorInputStream)
                .close();

        assertThatThrownBy(() -> deflateDecompressor.close())
                .isInstanceOf(IOException.class)
                .hasMessage("Failed to close deflate stream");
        verify(mockedDeflateCompressorInputStream, times(1)).close();
    }
}
