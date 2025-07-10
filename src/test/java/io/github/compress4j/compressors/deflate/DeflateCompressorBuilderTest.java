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
import static org.mockito.Mockito.mock;

import java.io.IOException;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.junit.jupiter.api.Test;

class DeflateCompressorBuilderTest {

    @Test
    void shouldBuildOutputStream() throws IOException {
        // given
        var outputStream = mock(DeflateCompressorOutputStream.class);
        var builder = DeflateCompressor.builder(outputStream);
        // when
        try (DeflateCompressorOutputStream out = builder.buildCompressorOutputStream()) {

            // then
            assertThat(out)
                    .isNotNull()
                    .extracting("deflater")
                    .extracting("level", "strategy")
                    .containsExactly(DEFAULT_COMPRESSION, DEFAULT_STRATEGY);
        }
    }

    @Test
    void shouldBuildOutputStreamWithDeflateParameters() throws IOException {
        // given
        var outputStream = mock(DeflateCompressorOutputStream.class);

        DeflateCompressionLevel deflateCompressionLevel = DeflateCompressionLevel.HUFFMAN_ONLY;
        boolean zLibCompress = true;

        var builder = DeflateCompressor.builder(outputStream)
                .compressorOutputStreamBuilder()
                .setCompressionLevel(deflateCompressionLevel)
                .setZlibHeader(zLibCompress)
                .parentBuilder();
        // when
        try (DeflateCompressorOutputStream out = builder.buildCompressorOutputStream()) {

            // then
            assertThat(out)
                    .isNotNull()
                    .extracting("deflater")
                    .extracting("level", "strategy")
                    .containsExactly(HUFFMAN_ONLY, DEFAULT_STRATEGY);
        }
    }
}
