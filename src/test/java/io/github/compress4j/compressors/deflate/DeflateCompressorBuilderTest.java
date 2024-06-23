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

import static java.util.zip.Deflater.DEFAULT_COMPRESSION;
import static java.util.zip.Deflater.DEFAULT_STRATEGY;
import static java.util.zip.Deflater.HUFFMAN_ONLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeflateCompressorBuilderTest {

    @Mock
    private OutputStream mockOutputStream;

    private DeflateCompressor.DeflateCompressorBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DeflateCompressor.DeflateCompressorBuilder(mockOutputStream);
    }

    @Test
    @DisplayName("Should construct CompressorBuilder with OutputStream")
    void constructor_WithOutputStream_SetsField() throws NoSuchFieldException, IllegalAccessException {
        Field outputStreamField =
                DeflateCompressor.DeflateCompressorBuilder.class.getSuperclass().getDeclaredField("outputStream");
        outputStreamField.setAccessible(true);
        OutputStream actualOutputStream = (OutputStream) outputStreamField.get(builder);

        assertThat(builder).isNotNull();
        assertThat(actualOutputStream).isEqualTo(mockOutputStream);
    }

    @Test
    @DisplayName("Should return the builder instance itself from getThis()")
    void getThis_ReturnsThisInstance() {
        assertThat(builder.getThis()).isEqualTo(builder);
    }

    @Test
    @DisplayName("Should build a DeflateCompressorOutputStream instance")
    void buildCompressorOutputStream_ReturnsCompressorOutputStreamInstance() throws IOException {
        DeflateCompressorOutputStream compressorOutputStream = builder.buildCompressorOutputStream();

        assertThat(compressorOutputStream)
                .isInstanceOf(DeflateCompressorOutputStream.class)
                .isNotNull();
    }

    @Test
    @DisplayName("Should build a DeflateCompressor instance")
    void build_ReturnsCompressorInstance() throws IOException {
        DeflateCompressor compressor = builder.build();

        assertThat(compressor).isInstanceOf(DeflateCompressor.class).isNotNull();
    }

    @Test
    @DisplayName("Should throw IOException when building CompressorOutputStream fails")
    void buildCompressorOutputStream_ThrowsIOException_WhenOutputStreamFails() {
        OutputStream throwingOutputStream = mock(OutputStream.class);
        DeflateCompressor.DeflateCompressorBuilder failingBuilder =
                new DeflateCompressor.DeflateCompressorBuilder(throwingOutputStream) {
                    @Override
                    public DeflateCompressorOutputStream buildCompressorOutputStream() throws IOException {
                        throw new IOException("Simulated CompressorOutputStream build error");
                    }
                };

        assertThatThrownBy(failingBuilder::buildCompressorOutputStream)
                .isInstanceOf(IOException.class)
                .hasMessage("Simulated CompressorOutputStream build error");
    }

    @Test
    @DisplayName("Should throw IOException when building Compressor fails due to CompressorOutputStream build error")
    void build_ThrowsIOException_WhenCompressorOutputStreamBuildFails() {
        OutputStream throwingOutputStream = mock(OutputStream.class);
        DeflateCompressor.DeflateCompressorBuilder failingBuilder =
                new DeflateCompressor.DeflateCompressorBuilder(throwingOutputStream) {
                    @Override
                    public DeflateCompressorOutputStream buildCompressorOutputStream() throws IOException {
                        throw new IOException("Simulated CompressorOutputStream build error during Compressor build");
                    }
                };

        assertThatThrownBy(failingBuilder::build)
                .isInstanceOf(IOException.class)
                .hasMessage("Simulated CompressorOutputStream build error during Compressor build");
    }

    @Test
    @DisplayName("Should build OutputStream with compression level set")
    void shouldBuildOutputStreamWithCompressionLevelSet()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        DeflateCompressor.DeflateOutputStreamBuilder<DeflateCompressor.DeflateCompressorBuilder>
                compressorOutputStreamBuilder = spy(builder.compressorOutputStreamBuilder());

        DeflateCompressionLevel testCompressionLevel = DeflateCompressionLevel.BEST_SPEED;
        compressorOutputStreamBuilder.setCompressionLevel(testCompressionLevel);

        Field compressionLevelField =
                DeflateCompressor.DeflateOutputStreamBuilder.class.getDeclaredField("compressionLevel");
        compressionLevelField.setAccessible(true);
        int actualCompressionLevel = (int) compressionLevelField.get(compressorOutputStreamBuilder);

        assertThat(actualCompressionLevel).isEqualTo(testCompressionLevel.getValue());

        DeflateCompressorOutputStream mockDeflateCompressorOutputStream = mock(DeflateCompressorOutputStream.class);
        when(compressorOutputStreamBuilder.buildOutputStream()).thenReturn(mockDeflateCompressorOutputStream);

        try (DeflateCompressorOutputStream buildCompressorOutputStream =
                compressorOutputStreamBuilder.buildOutputStream()) {
            assertThat(buildCompressorOutputStream).isInstanceOf(DeflateCompressorOutputStream.class);
        }
    }

    @Test
    @DisplayName("Should build OutputStream with zlib header set")
    void shouldBuildOutputStreamWithZlibHeaderSet() throws IOException, NoSuchFieldException, IllegalAccessException {
        DeflateCompressor.DeflateOutputStreamBuilder<DeflateCompressor.DeflateCompressorBuilder>
                compressorOutputStreamBuilder = spy(builder.compressorOutputStreamBuilder());

        compressorOutputStreamBuilder.setZlibHeader(false);

        Field zlibHeaderField = DeflateCompressor.DeflateOutputStreamBuilder.class.getDeclaredField("zlibHeader");
        zlibHeaderField.setAccessible(true);
        boolean actualZlibHeader = (boolean) zlibHeaderField.get(compressorOutputStreamBuilder);

        assertThat(actualZlibHeader).isFalse();

        DeflateCompressorOutputStream mockDeflateCompressorOutputStream = mock(DeflateCompressorOutputStream.class);
        when(compressorOutputStreamBuilder.buildOutputStream()).thenReturn(mockDeflateCompressorOutputStream);

        try (DeflateCompressorOutputStream buildCompressorOutputStream =
                compressorOutputStreamBuilder.buildOutputStream()) {
            assertThat(buildCompressorOutputStream).isInstanceOf(DeflateCompressorOutputStream.class);
        }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid compression level (less than 0)")
    void setCompressionLevel_ThrowsIllegalArgumentException_WhenLessThanZero() {
        DeflateCompressor.DeflateOutputStreamBuilder<DeflateCompressor.DeflateCompressorBuilder>
                compressorOutputStreamBuilder = builder.compressorOutputStreamBuilder();

        DeflateCompressionLevel invalidLevel = DeflateCompressionLevel.DEFAULT_COMPRESSION;

        assertThatThrownBy(() -> compressorOutputStreamBuilder.setCompressionLevel(invalidLevel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Deflate compression level: " + invalidLevel);
    }

    @Test
    void shouldBuildOutputStream() throws IOException {
        var outputStream = mock(OutputStream.class);
        try (DeflateCompressorOutputStream out =
                DeflateCompressor.builder(outputStream).buildCompressorOutputStream()) {
            assertThat(out)
                    .isNotNull()
                    .extracting("deflater")
                    .extracting("level", "strategy")
                    .containsExactly(DEFAULT_COMPRESSION, DEFAULT_STRATEGY);
        }
    }

    @Test
    void shouldBuildOutputStreamWithDeflateParameters() throws IOException {
        var outputStream = mock(OutputStream.class);

        DeflateCompressionLevel deflateCompressionLevel = DeflateCompressionLevel.HUFFMAN_ONLY;
        boolean zLibCompress = true;

        var deflateCompressorBuilder = DeflateCompressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .setCompressionLevel(deflateCompressionLevel)
                .setZlibHeader(zLibCompress)
                .parentBuilder();
        try (DeflateCompressorOutputStream out = deflateCompressorBuilder.buildCompressorOutputStream()) {
            assertThat(out)
                    .isNotNull()
                    .extracting("deflater")
                    .extracting("level", "strategy")
                    .containsExactly(HUFFMAN_ONLY, DEFAULT_STRATEGY);
        }
    }
}
