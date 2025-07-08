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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.compress4j.compressors.gzip.GzipCompressor.GzipCompressorBuilder;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GzipCompressorTest {

    @Mock
    private GzipCompressorOutputStream mockGzipCompressorOutputStream;

    @Mock
    private OutputStream mockOutputStream;

    @Test
    void shouldConstructWithGzipCompressorOutputStream() {
        // When
        GzipCompressor compressor = new GzipCompressor(mockGzipCompressorOutputStream);

        // Then
        assertThat(compressor).isNotNull();
    }

    @Test
    void shouldConstructWithGzipCompressorBuilder() throws IOException {
        // Given
        GzipCompressorBuilder mockBuilder = mock(GzipCompressorBuilder.class);
        when(mockBuilder.buildCompressorOutputStream()).thenReturn(mockGzipCompressorOutputStream);

        // When
        GzipCompressor compressor = new GzipCompressor(mockBuilder);

        // Then
        assertThat(compressor).isNotNull();
        //noinspection resource
        verify(mockBuilder).buildCompressorOutputStream();
    }

    @Test
    void shouldReturnBuilderWithCorrectPath() throws IOException {
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class)) {
            // Given
            Path mockPath = mock(Path.class);
            //noinspection resource
            mockFiles.when(() -> Files.newOutputStream(mockPath)).thenReturn(mockOutputStream);

            // When
            GzipCompressorBuilder builder = GzipCompressor.builder(mockPath);

            // Then
            assertThat(builder).isNotNull();
        }
    }

    @Test
    void shouldWritePathEntry() throws Exception {
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class)) {
            // Given
            Path mockPath = mock(Path.class);

            // When
            new GzipCompressor(mockGzipCompressorOutputStream).write(mockPath);

            // Then
            mockFiles.verify(() -> Files.copy(mockPath, mockGzipCompressorOutputStream));
        }
    }

    @Test
    void shouldWriteFileEntry() throws Exception {
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class)) {
            // Given
            File mockFile = mock(File.class);
            Path mockPath = mock(Path.class);
            when(mockFile.toPath()).thenReturn(mockPath);

            // When
            new GzipCompressor(mockGzipCompressorOutputStream).write(mockFile);

            // Then
            mockFiles.verify(() -> Files.copy(mockPath, mockGzipCompressorOutputStream));
        }
    }

    @Test
    void shouldCloseOutputStreamOnClose() throws IOException {
        // Given
        GzipCompressor compressor = new GzipCompressor(mockGzipCompressorOutputStream);

        // When
        compressor.close();

        // Then
        verify(mockGzipCompressorOutputStream).close();
    }
}
