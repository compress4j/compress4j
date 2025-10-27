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
package io.github.compress4j.compressors.pack200;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Pack200CompressorTest {

    @Mock
    private Pack200CompressorOutputStream mockPack200CompressorOutputStream;

    @Mock
    private OutputStream mockOutputStream;

    @Test
    void shouldConstructWithPack200CompressorOutputStream() {
        // When
        Pack200Compressor compressor = new Pack200Compressor(mockPack200CompressorOutputStream);

        // Then
        assertThat(compressor).isNotNull();
    }

    @Test
    void shouldConstructWithPack200CompressorBuilder() throws IOException {
        // Given
        Pack200Compressor.Pack200CompressorBuilder mockBuilder = mock(Pack200Compressor.Pack200CompressorBuilder.class);
        when(mockBuilder.buildCompressorOutputStream()).thenReturn(mockPack200CompressorOutputStream);

        // When
        Pack200Compressor compressor = new Pack200Compressor(mockBuilder);

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
            Pack200Compressor.Pack200CompressorBuilder builder = Pack200Compressor.builder(mockPath);

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
            new Pack200Compressor(mockPack200CompressorOutputStream).write(mockPath);

            // Then
            mockFiles.verify(() -> Files.copy(mockPath, mockPack200CompressorOutputStream));
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
            new Pack200Compressor(mockPack200CompressorOutputStream).write(mockFile);

            // Then
            mockFiles.verify(() -> Files.copy(mockPath, mockPack200CompressorOutputStream));
        }
    }

    @Test
    void shouldCloseOutputStreamOnClose() throws IOException {
        // Given
        Pack200Compressor compressor = new Pack200Compressor(mockPack200CompressorOutputStream);

        // When
        compressor.close();

        // Then
        verify(mockPack200CompressorOutputStream).close();
    }
}
