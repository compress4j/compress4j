/*
 * Copyright 2025 The Compress4J Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.compress4j.compressors.deflate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;

import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

class DeflateDecompressorBuilderTest {

    @Mock
    private DeflateCompressorInputStream mockDeflateCompressorInputStream;

    @Mock
    private InputStream mockRawInputStream;

    @TempDir
    Path tempDir;

    @Test
    void whenBuilderGivenPathConstructsDecompressor() throws IOException {
        DeflateDecompressor.DeflateDecompressorBuilder builder = spy(new DeflateDecompressor.DeflateDecompressorBuilder(tempDir.toFile())); // Changed class name
        doReturn(mockDeflateCompressorInputStream).when(builder).buildCompressorInputStream();

        DeflateDecompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenFileConstructsDecompressor() throws IOException {
        DeflateDecompressor.DeflateDecompressorBuilder builder = spy(new DeflateDecompressor.DeflateDecompressorBuilder(tempDir)); // Changed class name
        doReturn(mockDeflateCompressorInputStream).when(builder).buildCompressorInputStream();

        DeflateDecompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenInputStreamConstructsDecompressor() throws IOException {
        DeflateDecompressor.DeflateDecompressorBuilder builder = spy(new DeflateDecompressor.DeflateDecompressorBuilder(mockRawInputStream)); // Changed class name
        doReturn(mockDeflateCompressorInputStream).when(builder).buildCompressorInputStream();

        DeflateDecompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenWritingInputStreamFailsThrowIOExeption() throws IOException {
        DeflateDecompressor.DeflateDecompressorBuilder builder = spy(new DeflateDecompressor.DeflateDecompressorBuilder(mockRawInputStream)); // Changed class name
        doThrow(new IOException()).when(builder).buildCompressorInputStream();

        assertThrows(IOException.class, builder::buildCompressorInputStream);
    }

    @Test
    void shouldBuildInputStream() throws IOException {
        DeflateDecompressor.DeflateDecompressorBuilder parentBuilder = new DeflateDecompressor.DeflateDecompressorBuilder(mockRawInputStream); // Changed class name

        DeflateDecompressor.DeflateDecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(new DeflateDecompressor.DeflateDecompressorInputStreamBuilder(parentBuilder, mockRawInputStream)); // Changed class name

        doReturn(mock(DeflateCompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();

        // when
        try (DeflateCompressorInputStream buildCompresserInputStream = compressorInputStreamBuilder.buildInputStream()) { // Changed class type
            // then
            assertThat(buildCompresserInputStream).isInstanceOf(DeflateCompressorInputStream.class); // Changed class type
        }
    }

    @Test
    void shouldBuildInputStreamWithZlibHeaderTrue()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        DeflateDecompressor.DeflateDecompressorBuilder parentBuilder = new DeflateDecompressor.DeflateDecompressorBuilder(mockRawInputStream); // Changed class name

        DeflateDecompressor.DeflateDecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(parentBuilder.inputStreamBuilder());

        doReturn(mock(DeflateCompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();
        compressorInputStreamBuilder.setWithZlibHeader(true);

        Field withZlibHeaderField =
                DeflateDecompressor.DeflateDecompressorInputStreamBuilder.class.getDeclaredField("withZlibHeader"); // Changed class name and field name
        withZlibHeaderField.setAccessible(true);

        boolean withZlibHeaderValue = (boolean) withZlibHeaderField.get(compressorInputStreamBuilder);

        assertThat(withZlibHeaderValue).isTrue();

        // when
        try (DeflateCompressorInputStream buildCompresserInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompresserInputStream).isInstanceOf(DeflateCompressorInputStream.class);
        }
    }
}
