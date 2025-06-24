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

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.junit.jupiter.api.Test;

class   BZip2CompressorBuilderTest {

    @Test
    void shouldBuildArchiveOutputStream() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = BZip2Compressor.builder(outputStream);

        // when
        try (BZip2CompressorOutputStream out = builder.buildCompressorOutputStream()) {

            // then
            assertThat(out).isNotNull().extracting("blockSize100k").isEqualTo(9);
        }
    }

    @Test
    void shouldBuildArchiveOutputStreamWithBlockSize() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = BZip2Compressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .blockSize(5)
                .parentBuilder();

        // when
        try (BZip2CompressorOutputStream out = builder.buildCompressorOutputStream()) {

            // then
            assertThat(out).isNotNull().extracting("blockSize100k").isEqualTo(5);
        }
    }

    @Test
    void shouldNotAllowBlockSizeLowerOutOfRange() {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = BZip2Compressor.builder(outputStream).compressorOutputStreamBuilder();

        // when & then
        assertThatThrownBy(() -> builder.blockSize(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockSize(-2) < 1");
    }

    @Test
    void shouldNotAllowBlockSizeHigherOutOfRange() {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = BZip2Compressor.builder(outputStream).compressorOutputStreamBuilder();

        // when & then
        assertThatThrownBy(() -> builder.blockSize(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockSize(10) > 9");
    }
}
