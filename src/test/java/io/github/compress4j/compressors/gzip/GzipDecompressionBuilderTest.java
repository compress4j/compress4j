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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

public class GzipDecompressionBuilderTest {

    @Mock
    private GzipCompressorInputStream mockGzipCompressorInputStream;

    @Mock
    private InputStream mockRawInputStream;

    @TempDir
    Path tempDir;

    @Test
    void whenBuilderGivenPathConstructsDecompressor() throws IOException {
        GZipDecompressor.GZipDecompressorBuilder builder =
                spy(new GZipDecompressor.GZipDecompressorBuilder(tempDir.toFile()));
        doReturn(mockGzipCompressorInputStream).when(builder).buildCompressorInputStream();

        GZipDecompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenFileConstructsDecompressor() throws IOException {
        GZipDecompressor.GZipDecompressorBuilder builder = spy(new GZipDecompressor.GZipDecompressorBuilder(tempDir));
        doReturn(mockGzipCompressorInputStream).when(builder).buildCompressorInputStream();

        GZipDecompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenInputStreamConstructsDecompressor() throws IOException {
        GZipDecompressor.GZipDecompressorBuilder builder =
                spy(new GZipDecompressor.GZipDecompressorBuilder(mockRawInputStream));
        doReturn(mockGzipCompressorInputStream).when(builder).buildCompressorInputStream();

        GZipDecompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenWritingInputStreamFailsThrowIOExeption() throws IOException {
        GZipDecompressor.GZipDecompressorBuilder builder =
                spy(new GZipDecompressor.GZipDecompressorBuilder(mockRawInputStream));
        doThrow(new IOException()).when(builder).buildCompressorInputStream();

        assertThrows(IOException.class, builder::buildCompressorInputStream);
    }

    @Test
    void shouldBuildInputStream() throws IOException {
        GZipDecompressor.GZipDecompressorBuilder parentBuilder =
                new GZipDecompressor.GZipDecompressorBuilder(mockRawInputStream);

        GZipDecompressor.GZipDecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(new GZipDecompressor.GZipDecompressorInputStreamBuilder(parentBuilder, mockRawInputStream));

        doReturn(mock(GzipCompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();

        // when
        try (GzipCompressorInputStream buildCompresserInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompresserInputStream).isInstanceOf(GzipCompressorInputStream.class);
        }
    }

    @Test
    void shouldBuildInputStreamWithDecompressConcatTrue()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        GZipDecompressor.GZipDecompressorBuilder parentBuilder =
                new GZipDecompressor.GZipDecompressorBuilder(mockRawInputStream);

        GZipDecompressor.GZipDecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(parentBuilder.inputStreamBuilder());

        doReturn(mock(GzipCompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();
        compressorInputStreamBuilder.setDecompressConcatenated(true);

        Field decompressConcatenatedField =
                GZipDecompressor.GZipDecompressorInputStreamBuilder.class.getDeclaredField("decompressConcatenated");
        decompressConcatenatedField.setAccessible(true);

        boolean decompressConcatenatedValue = (boolean) decompressConcatenatedField.get(compressorInputStreamBuilder);

        assertThat(decompressConcatenatedValue).isTrue();

        // when
        try (GzipCompressorInputStream buildCompresserInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompresserInputStream).isInstanceOf(GzipCompressorInputStream.class);
        }
    }
}
