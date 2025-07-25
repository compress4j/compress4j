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
package io.github.compress4j.compressors.bzip2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.compress4j.compressors.bzip2.BZip2Decompressor.BZip2DecompressorBuilder;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BZip2DecompressorTest {

    @Mock
    private BZip2CompressorInputStream mockBZip2CompressorInputStream;

    private BZip2Decompressor bzip2Decompressor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        bzip2Decompressor = new BZip2Decompressor(mockBZip2CompressorInputStream);
    }

    @Test
    @DisplayName("Should construct BZip2Decompressor with a BZip2DecompressorBuilder")
    void constructor_WithBuilder_SetsField() throws IOException {
        // given
        BZip2DecompressorBuilder mockBuilder = mock(BZip2DecompressorBuilder.class);

        // when
        BZip2Decompressor decompressorFromBuilder = new BZip2Decompressor(mockBuilder);

        // then
        assertThat(decompressorFromBuilder).isNotNull();
    }

    @Test
    @DisplayName("Should write all bytes from input stream to a file")
    void write_ToFile_CopiesBytes() throws IOException {
        // given
        Path outputPath = tempDir.resolve("output.txt");
        byte[] testBytes = "BZip2 Decompressor Test Data".getBytes();

        when(mockBZip2CompressorInputStream.transferTo(any(OutputStream.class))).thenAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(testBytes);
            return ((Number) testBytes.length).longValue();
        });

        // When
        long bytesWritten = bzip2Decompressor.write(outputPath.toFile());

        // Then
        assertThat(outputPath).exists().hasContent("BZip2 Decompressor Test Data");
        assertThat(bytesWritten).isEqualTo(testBytes.length);
    }

    @Test
    @DisplayName("Should write all bytes from input stream to a path")
    void write_ToPath_CopiesBytes() throws IOException {
        // Given
        Path outputPath = tempDir.resolve("output.txt");
        byte[] testBytes = "More BZip2 Decompressor Test Data".getBytes();

        when(mockBZip2CompressorInputStream.transferTo(any(OutputStream.class))).thenAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(testBytes);
            return ((Number) testBytes.length).longValue();
        });

        // When
        long bytesWritten = bzip2Decompressor.write(outputPath);

        // Then
        assertThat(outputPath).exists().hasContent("More BZip2 Decompressor Test Data");
        assertThat(bytesWritten).isEqualTo(testBytes.length);
    }

    @Test
    @DisplayName("Should throw IOException when writing to file fails")
    void write_ToFile_ThrowsIOException_WhenCopyFails() {
        File nonWritableFile = new File("/nonexistent/path/cannot_write.txt");

        assertThatThrownBy(() -> bzip2Decompressor.write(nonWritableFile))
                .isInstanceOf(IOException.class)
                .hasMessage(nonWritableFile.toString());
    }

    @Test
    @DisplayName("Should throw IOException when writing to path fails")
    void write_ToPath_ThrowsIOException_WhenCopyFails() {
        Path nonWritablePath = tempDir.resolve("non_existent_dir/output.txt");

        assertThatThrownBy(() -> bzip2Decompressor.write(nonWritablePath))
                .isInstanceOf(IOException.class)
                .hasMessage(nonWritablePath.toString());
    }

    @Test
    @DisplayName("Should close the BZip2 compressor input stream")
    void close_ClosesBZip2CompressorInputStream() throws IOException {
        bzip2Decompressor.close();
        verify(mockBZip2CompressorInputStream, times(1)).close();
    }

    @Test
    @DisplayName("Should throw IOException when closing BZip2 compressor input stream fails")
    void close_ThrowsIOException_WhenBZip2CompressorInputStreamCloseFails() throws IOException {
        doThrow(new IOException("Failed to close BZip2 stream"))
                .when(mockBZip2CompressorInputStream)
                .close();

        assertThatThrownBy(() -> bzip2Decompressor.close())
                .isInstanceOf(IOException.class)
                .hasMessage("Failed to close BZip2 stream");
        verify(mockBZip2CompressorInputStream, times(1)).close();
    }
}
