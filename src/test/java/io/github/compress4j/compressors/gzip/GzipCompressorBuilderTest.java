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

import static java.util.zip.Deflater.BEST_COMPRESSION;
import static java.util.zip.Deflater.HUFFMAN_ONLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;

class GzipCompressorBuilderTest {

    @Test
    void shouldBuildArchiveOutputStream() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = GzipCompressor.builder(outputStream);

        // when
        try (GzipCompressorOutputStream out = builder.buildCompressorOutputStream()) {

            // then
            assertThat(out).isNotNull();
        }
    }

    @Test
    void shouldBuildArchiveOutputStreamWithGzipParameters() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var now = Instant.now();
        var builder = GzipCompressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .bufferSize(1024)
                .compressionLevel(BEST_COMPRESSION)
                .comment("comment")
                .deflateStrategy(HUFFMAN_ONLY)
                .fileName("test.tar.gz")
                .modificationTime(now.toEpochMilli())
                .operatingSystem(0)
                .parentBuilder();

        // when
        try (GzipCompressorOutputStream out = builder.buildCompressorOutputStream()) {

            // then
            assertThat(out).isNotNull().extracting("deflateBuffer").isEqualTo(new byte[1024]);
            assertThat(out)
                    .extracting("deflater")
                    .extracting("level", "strategy")
                    .containsExactly(BEST_COMPRESSION, HUFFMAN_ONLY);
        }
    }

    @Test
    void shouldNotAllowIncorrectBufferSize() {
        // given
        var mockOutputStream = mock(OutputStream.class);
        var builder = GzipCompressor.builder(mockOutputStream).compressorOutputStreamBuilder();

        // when & then
        assertThatThrownBy(() -> builder.bufferSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid buffer size: 0");
    }

    @Test
    void shouldNotAllowCompressionLevelLowerOutOfRange() {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = GzipCompressor.builder(outputStream).compressorOutputStreamBuilder();

        // when & then
        assertThatThrownBy(() -> builder.compressionLevel(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid gzip compression level: -2");
    }

    @Test
    void shouldNotAllowCompressionLevelHigherOutOfRange() {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = GzipCompressor.builder(outputStream).compressorOutputStreamBuilder();

        // when & then
        assertThatThrownBy(() -> builder.compressionLevel(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid gzip compression level: 10");
    }
}
