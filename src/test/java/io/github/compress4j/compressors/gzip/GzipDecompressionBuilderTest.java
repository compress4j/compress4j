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

import io.github.compress4j.compressors.gzip.GzipDecompressor.GzipDecompressorBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

class GzipDecompressionBuilderTest {

    @Mock
    private GzipCompressorInputStream mockGzipCompressorInputStream;

    @Mock
    private InputStream mockRawInputStream;

    @Test
    void whenBuilderGivenPathConstructsDecompressor(@TempDir Path tempDir) throws IOException {
        // given
        File targetPath = tempDir.resolve("target").toFile();
        Files.createFile(targetPath.toPath());
        GzipDecompressorBuilder builder = spy(new GzipDecompressorBuilder(targetPath));
        //noinspection resource
        doReturn(mockGzipCompressorInputStream).when(builder).buildCompressorInputStream();

        // when
        GzipDecompressor actual = builder.build();

        // then
        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenFileConstructsDecompressor(@TempDir Path tempDir) throws IOException {
        // given
        Path tartgetPath = tempDir.resolve("target");
        Files.createFile(tartgetPath);
        GzipDecompressorBuilder builder = spy(new GzipDecompressorBuilder(tartgetPath));
        //noinspection resource
        doReturn(mockGzipCompressorInputStream).when(builder).buildCompressorInputStream();

        // when
        GzipDecompressor actual = builder.build();

        // then
        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenInputStreamConstructsDecompressor() throws IOException {
        // given
        GzipDecompressorBuilder builder = spy(new GzipDecompressorBuilder(mockRawInputStream));
        //noinspection resource
        doReturn(mockGzipCompressorInputStream).when(builder).buildCompressorInputStream();

        // when
        GzipDecompressor actual = builder.build();

        // then
        assertThat(actual).isNotNull();
    }

    @Test
    void whenWritingInputStreamFailsThrowIOException() throws IOException {
        // given
        GzipDecompressorBuilder builder = spy(new GzipDecompressorBuilder(mockRawInputStream));
        //noinspection resource
        doThrow(new IOException()).when(builder).buildCompressorInputStream();

        // when & then
        assertThrows(IOException.class, builder::buildCompressorInputStream);
    }

    @Test
    void shouldBuildInputStream() throws IOException {
        // given
        GzipDecompressorBuilder parentBuilder = new GzipDecompressorBuilder(mockRawInputStream);

        GzipDecompressor.GzipDecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(new GzipDecompressor.GzipDecompressorInputStreamBuilder(parentBuilder, mockRawInputStream));
        //noinspection resource
        doReturn(mock(GzipCompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();

        // when
        try (GzipCompressorInputStream buildCompressorInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompressorInputStream).isInstanceOf(GzipCompressorInputStream.class);
        }
    }

    @Test
    void shouldBuildInputStreamWithDecompressConcatTrue()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        // given
        GzipDecompressorBuilder parentBuilder = new GzipDecompressorBuilder(mockRawInputStream);

        GzipDecompressor.GzipDecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(parentBuilder.inputStreamBuilder());

        //noinspection resource
        doReturn(mock(GzipCompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();
        compressorInputStreamBuilder.setDecompressConcatenated(true);

        Field decompressConcatenatedField =
                GzipDecompressor.GzipDecompressorInputStreamBuilder.class.getDeclaredField("decompressConcatenated");
        decompressConcatenatedField.setAccessible(true);

        boolean decompressConcatenatedValue = (boolean) decompressConcatenatedField.get(compressorInputStreamBuilder);

        assertThat(decompressConcatenatedValue).isTrue();

        // when
        try (GzipCompressorInputStream buildCompressorInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompressorInputStream).isInstanceOf(GzipCompressorInputStream.class);
        }
    }
}
