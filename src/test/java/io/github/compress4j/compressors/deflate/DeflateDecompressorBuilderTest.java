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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.github.compress4j.compressors.deflate.DeflateDecompressor.DeflateDecompressorBuilder;
import io.github.compress4j.compressors.deflate.DeflateDecompressor.DeflateDecompressorInputStreamBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
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

    @Test
    void whenBuilderGivenPathConstructsDecompressor(@TempDir Path tempDir) throws IOException {
        // given
        File targetPath = tempDir.resolve("target").toFile();
        Files.createFile(targetPath.toPath());
        DeflateDecompressorBuilder builder = spy(new DeflateDecompressorBuilder(targetPath));
        //noinspection resource
        doReturn(mockDeflateCompressorInputStream).when(builder).buildCompressorInputStream();

        // when
        DeflateDecompressor actual = builder.build();

        // then
        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenFileConstructsDecompressor(@TempDir Path tempDir) throws IOException {
        // given
        Path tartgetPath = tempDir.resolve("target");
        Files.createFile(tartgetPath);
        DeflateDecompressorBuilder builder = spy(new DeflateDecompressorBuilder(tartgetPath));
        //noinspection resource
        doReturn(mockDeflateCompressorInputStream).when(builder).buildCompressorInputStream();

        // when
        DeflateDecompressor actual = builder.build();

        // then
        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenInputStreamConstructsDecompressor() throws IOException {
        // given
        DeflateDecompressorBuilder builder = spy(new DeflateDecompressorBuilder(mockRawInputStream));
        //noinspection resource
        doReturn(mockDeflateCompressorInputStream).when(builder).buildCompressorInputStream();

        // when
        DeflateDecompressor actual = builder.build();

        // then
        assertThat(actual).isNotNull();
    }

    @Test
    void whenWritingInputStreamFailsThrowIOException() {
        // given
        DeflateDecompressorBuilder builder = spy(new DeflateDecompressorBuilder(mockRawInputStream));
        //noinspection resource
        doThrow(new IOException()).when(builder).buildCompressorInputStream();

        // when & then
        assertThrows(IOException.class, builder::buildCompressorInputStream);
    }

    @Test
    void shouldBuildInputStream() throws IOException {
        // given
        DeflateDecompressorBuilder parentBuilder = new DeflateDecompressorBuilder(mockRawInputStream);

        DeflateDecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(new DeflateDecompressorInputStreamBuilder(parentBuilder, mockRawInputStream));

        //noinspection resource
        doReturn(mock(DeflateCompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();

        // when
        try (DeflateCompressorInputStream buildCompressorInputStream =
                compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompressorInputStream).isInstanceOf(DeflateCompressorInputStream.class);
        }
    }

    @Test
    void shouldBuildInputStreamWithZlibHeaderTrue() throws IOException, NoSuchFieldException, IllegalAccessException {
        // given
        DeflateDecompressorBuilder parentBuilder = new DeflateDecompressorBuilder(mockRawInputStream);

        DeflateDecompressorInputStreamBuilder compressorInputStreamBuilder = spy(parentBuilder.inputStreamBuilder());

        //noinspection resource
        doReturn(mock(DeflateCompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();
        compressorInputStreamBuilder.setWithZlibHeader(true);

        Field withZlibHeaderField = DeflateDecompressorInputStreamBuilder.class.getDeclaredField("withZlibHeader");
        withZlibHeaderField.setAccessible(true);

        boolean withZlibHeaderValue = (boolean) withZlibHeaderField.get(compressorInputStreamBuilder);

        assertThat(withZlibHeaderValue).isTrue();

        // when
        try (DeflateCompressorInputStream buildCompressorInputStream =
                compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompressorInputStream).isInstanceOf(DeflateCompressorInputStream.class);
        }
    }
}
