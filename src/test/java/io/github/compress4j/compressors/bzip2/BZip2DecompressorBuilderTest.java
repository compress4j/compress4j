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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.github.compress4j.compressors.bzip2.BZip2Decompressor.BZip2DecompressorBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BZip2DecompressorBuilderTest {

    @Mock
    private BZip2CompressorInputStream mockBzip2CompressorInputStream;

    @Mock
    private InputStream mockRawInputStream;

    @TempDir
    Path tempDir;

    @Test
    void whenBuilderGivenPathConstructsDecompressor() throws IOException {
        BZip2DecompressorBuilder builder = spy(new BZip2DecompressorBuilder(tempDir.toFile()));
        doReturn(mockBzip2CompressorInputStream).when(builder).buildCompressorInputStream();

        BZip2Decompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenFileConstructsDecompressor() throws IOException {
        BZip2DecompressorBuilder builder = spy(new BZip2DecompressorBuilder(tempDir));
        doReturn(mockBzip2CompressorInputStream).when(builder).buildCompressorInputStream();

        BZip2Decompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenBuilderGivenInputStreamConstructsDecompressor() throws IOException {
        BZip2DecompressorBuilder builder = spy(new BZip2DecompressorBuilder(mockRawInputStream));
        doReturn(mockBzip2CompressorInputStream).when(builder).buildCompressorInputStream();

        BZip2Decompressor actual = builder.build();

        assertThat(actual).isNotNull();
    }

    @Test
    void whenWritingInputStreamFailsThrowIOExeption() throws IOException {
        BZip2DecompressorBuilder builder = spy(new BZip2DecompressorBuilder(mockRawInputStream));
        doThrow(new IOException()).when(builder).buildCompressorInputStream();

        assertThrows(IOException.class, builder::buildCompressorInputStream);
    }

    @Test
    void shouldBuildInputStream() throws IOException {
        BZip2DecompressorBuilder parentBuilder = new BZip2DecompressorBuilder(mockRawInputStream);

        BZip2Decompressor.BZip2DecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(new BZip2Decompressor.BZip2DecompressorInputStreamBuilder<>(parentBuilder, mockRawInputStream));

        doReturn(mock(BZip2CompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();

        // when
        try (BZip2CompressorInputStream buildCompresserInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompresserInputStream).isInstanceOf(BZip2CompressorInputStream.class);
        }
    }

    @Test
    void shouldBuildInputStreamWithDecompressConcatTrue()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        BZip2DecompressorBuilder parentBuilder = new BZip2DecompressorBuilder(mockRawInputStream);

        BZip2Decompressor.BZip2DecompressorInputStreamBuilder<BZip2DecompressorBuilder> compressorInputStreamBuilder =
                spy(parentBuilder.inputStreamBuilder());

        doReturn(mock(BZip2CompressorInputStream.class))
                .when(compressorInputStreamBuilder)
                .buildInputStream();
        compressorInputStreamBuilder.setDecompressConcatenated(true);

        Field decompressConcatenatedField =
                BZip2Decompressor.BZip2DecompressorInputStreamBuilder.class.getDeclaredField("decompressConcatenated");
        decompressConcatenatedField.setAccessible(true);

        boolean decompressConcatenatedValue = (boolean) decompressConcatenatedField.get(compressorInputStreamBuilder);

        assertThat(decompressConcatenatedValue).isTrue();

        // when
        try (BZip2CompressorInputStream buildCompresserInputStream = compressorInputStreamBuilder.buildInputStream()) {
            // then
            assertThat(buildCompresserInputStream).isInstanceOf(BZip2CompressorInputStream.class);
        }
    }
}
