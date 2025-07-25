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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BZip2CompressorBuilderTest {

    @Mock
    private OutputStream mockOutputStream;

    private BZip2Compressor.BZip2CompressorBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new BZip2Compressor.BZip2CompressorBuilder(mockOutputStream);
    }

    @Test
    @DisplayName("Should construct CompressorBuilder with OutputStream")
    void constructor_WithOutputStream_SetsField() throws NoSuchFieldException, IllegalAccessException {
        Field outputStreamField =
                BZip2Compressor.BZip2CompressorBuilder.class.getSuperclass().getDeclaredField("outputStream");
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
    @DisplayName("Should build a BZip2CompressorOutputStream instance")
    void buildCompressorOutputStream_ReturnsCompressorOutputStreamInstance() throws IOException {
        BZip2CompressorOutputStream compressorOutputStream = builder.buildCompressorOutputStream();

        assertThat(compressorOutputStream).isNotNull().isInstanceOf(BZip2CompressorOutputStream.class);
    }

    @Test
    @DisplayName("Should build a BZip2Compressor instance")
    void build_ReturnsCompressorInstance() throws IOException {
        BZip2Compressor compressor = builder.build();

        assertThat(compressor).isNotNull().isInstanceOf(BZip2Compressor.class);
    }

    @Test
    @DisplayName("Should throw IOException when building CompressorOutputStream fails")
    void buildCompressorOutputStream_ThrowsIOException_WhenOutputStreamFails() {
        OutputStream throwingOutputStream = mock(OutputStream.class);
        BZip2Compressor.BZip2CompressorBuilder failingBuilder =
                new BZip2Compressor.BZip2CompressorBuilder(throwingOutputStream) {
                    @Override
                    public BZip2CompressorOutputStream buildCompressorOutputStream() throws IOException {
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
        BZip2Compressor.BZip2CompressorBuilder failingBuilder =
                new BZip2Compressor.BZip2CompressorBuilder(throwingOutputStream) {
                    @Override
                    public BZip2CompressorOutputStream buildCompressorOutputStream() throws IOException {
                        throw new IOException("Simulated CompressorOutputStream build error during Compressor build");
                    }
                };

        assertThatThrownBy(failingBuilder::build)
                .isInstanceOf(IOException.class)
                .hasMessage("Simulated CompressorOutputStream build error during Compressor build");
    }

    @Test
    @DisplayName("Should build OutputStream with blockSize set")
    void shouldBuildOutputStreamWithBlockSizeSet() throws IOException, NoSuchFieldException, IllegalAccessException {
        BZip2Compressor.BZip2CompressorOutputStreamBuilder<BZip2Compressor.BZip2CompressorBuilder>
                compressorOutputStreamBuilder = spy(builder.compressorOutputStreamBuilder());

        int testBlockSize = 5;
        compressorOutputStreamBuilder.blockSize(testBlockSize);

        Field blockSizeField = BZip2Compressor.BZip2CompressorOutputStreamBuilder.class.getDeclaredField("blockSize");
        blockSizeField.setAccessible(true);
        int actualBlockSize = (int) blockSizeField.get(compressorOutputStreamBuilder);

        assertThat(actualBlockSize).isEqualTo(testBlockSize);

        BZip2CompressorOutputStream mockBZip2CompressorOutputStream = mock(BZip2CompressorOutputStream.class);
        when(compressorOutputStreamBuilder.build()).thenReturn(mockBZip2CompressorOutputStream);

        try (BZip2CompressorOutputStream buildCompressorOutputStream = compressorOutputStreamBuilder.build()) {
            assertThat(buildCompressorOutputStream).isInstanceOf(BZip2CompressorOutputStream.class);
        }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid blockSize (less than 1)")
    void blockSize_ThrowsIllegalArgumentException_WhenLessThanOne() {
        BZip2Compressor.BZip2CompressorOutputStreamBuilder<BZip2Compressor.BZip2CompressorBuilder>
                compressorOutputStreamBuilder = builder.compressorOutputStreamBuilder();

        assertThatThrownBy(() -> compressorOutputStreamBuilder.blockSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockSize(0) < 1");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid blockSize (greater than 9)")
    void blockSize_ThrowsIllegalArgumentException_WhenGreaterThanNine() {
        BZip2Compressor.BZip2CompressorOutputStreamBuilder<BZip2Compressor.BZip2CompressorBuilder>
                compressorOutputStreamBuilder = builder.compressorOutputStreamBuilder();

        assertThatThrownBy(() -> compressorOutputStreamBuilder.blockSize(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockSize(10) > 9");
    }

    @Test
    void shouldBuildArchiveOutputStream() throws IOException {
        var outputStream = mock(OutputStream.class);
        var compressorBuilder = BZip2Compressor.builder(outputStream);

        try (BZip2CompressorOutputStream out = compressorBuilder.buildCompressorOutputStream()) {
            assertThat(out).isNotNull().extracting("blockSize100k").isEqualTo(9);
        }
    }

    @Test
    void shouldBuildArchiveOutputStreamWithBlockSize() throws IOException {
        var outputStream = mock(OutputStream.class);
        var compressorBuilder = BZip2Compressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .blockSize(5)
                .parentBuilder();

        try (BZip2CompressorOutputStream out = compressorBuilder.buildCompressorOutputStream()) {
            assertThat(out).isNotNull().extracting("blockSize100k").isEqualTo(5);
        }
    }

    @Test
    void shouldNotAllowBlockSizeLowerOutOfRange() {
        var outputStream = mock(OutputStream.class);
        var compressorOutputStreamBuilder =
                BZip2Compressor.builder(outputStream).compressorOutputStreamBuilder();

        assertThatThrownBy(() -> compressorOutputStreamBuilder.blockSize(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockSize(-2) < 1");
    }

    @Test
    void shouldNotAllowBlockSizeHigherOutOfRange() {
        var outputStream = mock(OutputStream.class);
        var compressorOutputStreamBuilder =
                BZip2Compressor.builder(outputStream).compressorOutputStreamBuilder();

        assertThatThrownBy(() -> compressorOutputStreamBuilder.blockSize(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockSize(10) > 9");
    }
}
