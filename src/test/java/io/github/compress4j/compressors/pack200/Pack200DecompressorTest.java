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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Pack200DecompressorTest {

    @Mock
    private Pack200CompressorInputStream mockPack200CompressorInputStream;

    @Mock
    private InputStream mockInputStream;

    @Test
    void shouldConstructWithPack200CompressorInputStream() {
        // When
        Pack200Decompressor decompressor = new Pack200Decompressor(mockPack200CompressorInputStream);

        // Then
        assertThat(decompressor).isNotNull();
    }

    @Test
    void shouldConstructWithPack200DecompressorBuilder() throws IOException {
        // Given
        Pack200Decompressor.Pack200DecompressorBuilder mockBuilder =
                mock(Pack200Decompressor.Pack200DecompressorBuilder.class);
        when(mockBuilder.buildCompressorInputStream()).thenReturn(mockPack200CompressorInputStream);

        // When
        Pack200Decompressor decompressor = new Pack200Decompressor(mockBuilder);

        // Then
        assertThat(decompressor).isNotNull();
        //noinspection resource
        verify(mockBuilder).buildCompressorInputStream();
    }

    @Test
    void shouldReturnBuilderWithCorrectInputStream() {
        // When
        Pack200Decompressor.Pack200DecompressorBuilder builder = Pack200Decompressor.builder(mockInputStream);

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    void shouldReturnBuilderWithCorrectPath() throws IOException {
        try (MockedStatic<Files> mockFiles = mockStatic(Files.class)) {
            // Given
            Path mockPath = mock(Path.class);
            //noinspection resource
            mockFiles.when(() -> Files.newInputStream(mockPath)).thenReturn(mockInputStream);

            // When
            Pack200Decompressor.Pack200DecompressorBuilder builder = Pack200Decompressor.builder(mockPath);

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
            new Pack200Decompressor(mockPack200CompressorInputStream).write(mockPath);

            // Then
            mockFiles.verify(() -> Files.copy(mockPack200CompressorInputStream, mockPath));
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
            new Pack200Decompressor(mockPack200CompressorInputStream).write(mockFile);

            // Then
            mockFiles.verify(() -> Files.copy(mockPack200CompressorInputStream, mockPath));
        }
    }

    @Test
    void shouldCloseInputStreamOnClose() throws IOException {
        // Given
        Pack200Decompressor decompressor = new Pack200Decompressor(mockPack200CompressorInputStream);

        // When
        decompressor.close();

        // Then
        verify(mockPack200CompressorInputStream).close();
    }
}
