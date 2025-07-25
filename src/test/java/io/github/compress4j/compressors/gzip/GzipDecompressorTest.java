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
package io.github.compress4j.compressors.gzip;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GzipDecompressorTest {
    @Mock
    private GzipCompressorInputStream mockedGzipCompressorInputStream;

    private GzipDecompressor gZipDecompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gZipDecompressor = new GzipDecompressor(mockedGzipCompressorInputStream);
    }

    @Test
    @DisplayName("Should construct a Gzip decompressor using Builder")
    void shouldConstructGzipDecompressorUsingBuilder() throws IOException {

        InputStream mockRawInputStream = mock(InputStream.class);

        // *** CRITICAL ADDITION: Enable mark/reset support for the mock ***
        when(mockRawInputStream.markSupported()).thenReturn(true);

        // Stub the sequence of bytes that GzipCompressorInputStream's constructor expects
        // for a valid GZIP header (RFC 1952). This includes 10 bytes:
        // ID1, ID2, CM, FLG, MTIME(4 bytes), XFL, OS.
        when(mockRawInputStream.read())
                .thenReturn(31) // GZIP ID1 (0x1f)
                .thenReturn(139) // GZIP ID2 (0x8b)
                .thenReturn(8) // Compression Method (8 = Deflate)
                .thenReturn(0) // Flags (0 means no optional header fields)
                .thenReturn(0) // MTIME byte 1
                .thenReturn(0) // MTIME byte 2
                .thenReturn(0) // MTIME byte 3
                .thenReturn(0) // MTIME byte 4
                .thenReturn(0) // Extra flags (XFL)
                .thenReturn(0) // Operating System (OS)
                .thenReturn(-1); // After the 10 header bytes, signify End Of File.

        GzipDecompressor.GzipDecompressorBuilder mockBuilder =
                new GzipDecompressor.GzipDecompressorBuilder(mockRawInputStream);

        GzipDecompressor decompressorFromBuilder = new GzipDecompressor(mockBuilder);

        assertThat(decompressorFromBuilder).isNotNull();
    }

    @Test
    @DisplayName("Should write all bytes from input stream to a file")
    void write_ToFile_CopiesBytes() throws IOException {
        // given
        Path outputPath = tempDir.resolve("output.txt");
        byte[] testBytes = "Gzip is Great".getBytes();

        when(mockedGzipCompressorInputStream.transferTo(any(OutputStream.class)))
                .thenAnswer(invocation -> {
                    OutputStream outputStream = invocation.getArgument(0);
                    outputStream.write(testBytes);
                    return ((Number) testBytes.length).longValue();
                });

        // when
        long bytesWritten = gZipDecompressor.write(outputPath.toFile());

        // then
        assertThat(bytesWritten).isEqualTo(testBytes.length);
        assertThat(outputPath).exists().hasContent("Gzip is Great");
    }

    @Test
    @DisplayName("Should write all bytes from input stream to a path")
    void write_ToPath_CopiesBytes() throws IOException {
        // given
        Path outputPath = tempDir.resolve("output.txt");
        byte[] testBytes = "Gzip is Great".getBytes();

        when(mockedGzipCompressorInputStream.transferTo(any(OutputStream.class)))
                .thenAnswer(invocation -> {
                    OutputStream outputStream = invocation.getArgument(0);
                    outputStream.write(testBytes);
                    return ((Number) testBytes.length).longValue();
                });

        // when
        long bytesWritten = gZipDecompressor.write(outputPath);

        // then
        assertThat(bytesWritten).isEqualTo(testBytes.length);
        assertThat(outputPath).exists().hasContent("Gzip is Great");
    }

    @Test
    @DisplayName("Should throw IOException when writing to file fails")
    void write_ToFile_ThrowsIOException_WhenCopyFails() {
        File nonWritableFile = new File("/nonexistent/path/cannot_write.txt");

        assertThatThrownBy(() -> gZipDecompressor.write(nonWritableFile))
                .isInstanceOf(IOException.class)
                .hasMessage(nonWritableFile.toString());
    }

    @Test
    @DisplayName("Should throw IOException when writing to path fails")
    void write_ToPath_ThrowsIOException_WhenCopyFails() {
        Path nonWritablePath = tempDir.resolve("non_existent_dir/output.txt");

        assertThatThrownBy(() -> gZipDecompressor.write(nonWritablePath))
                .isInstanceOf(IOException.class)
                .hasMessage(nonWritablePath.toString());
    }

    @Test
    @DisplayName("Should close the gzip compressor input stream")
    void close_ClosesBZip2CompressorInputStream() throws IOException {
        gZipDecompressor.close();
        verify(mockedGzipCompressorInputStream, times(1)).close();
    }

    @Test
    @DisplayName("Should throw IOException when closing gzip compressor input stream fails")
    void close_ThrowsIOException_WhenBZip2CompressorInputStreamCloseFails() throws IOException {
        doThrow(new IOException("Failed to close BZip2 stream"))
                .when(mockedGzipCompressorInputStream)
                .close();

        assertThatThrownBy(() -> gZipDecompressor.close())
                .isInstanceOf(IOException.class)
                .hasMessage("Failed to close BZip2 stream");
        verify(mockedGzipCompressorInputStream, times(1)).close();
    }
}
