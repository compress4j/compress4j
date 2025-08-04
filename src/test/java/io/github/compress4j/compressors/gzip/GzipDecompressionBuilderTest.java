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

import static io.github.compress4j.test.util.GzipHelper.createGzipInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.github.compress4j.compressors.gzip.GzipDecompressor.GzipDecompressorBuilder;
import io.github.compress4j.compressors.gzip.GzipDecompressor.GzipDecompressorInputStreamBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.function.IOConsumer;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

class GzipDecompressionBuilderTest {

    private static final IOConsumer<GzipCompressorInputStream> NO_OP_CONSUMER = inputStream -> {
        // No operation consumer for testing purposes
    };

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

        GzipDecompressorInputStreamBuilder compressorInputStreamBuilder =
                spy(new GzipDecompressorInputStreamBuilder(parentBuilder, mockRawInputStream));
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
    void shouldBuildInputStreamWithParameters() throws IOException {
        // given
        var inputStream = createGzipInputStream("test content");
        var builder = GzipDecompressor.builder(inputStream)
                .compressorInputStreamBuilder()
                .setFileNameCharset(StandardCharsets.UTF_8)
                .setDecompressConcatenated(true)
                .setOnMemberStart(NO_OP_CONSUMER)
                .setOnMemberEnd(NO_OP_CONSUMER)
                .parentBuilder();

        // when
        try (GzipCompressorInputStream in = builder.buildCompressorInputStream()) {

            // then
            ObjectAssert<GzipCompressorInputStream> streamAssert = new ObjectAssert<>(in);
            streamAssert
                    .extracting("fileNameCharset", "decompressConcatenated", "onMemberStart", "onMemberEnd")
                    .containsExactly(StandardCharsets.UTF_8, true, NO_OP_CONSUMER, NO_OP_CONSUMER);
        }
    }
}
