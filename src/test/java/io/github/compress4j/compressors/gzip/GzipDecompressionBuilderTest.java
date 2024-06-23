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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GzipDecompressionBuilderTest {

    private InputStream mockRawInputStream;

    @BeforeEach
    void setUp() throws IOException {
        mockRawInputStream = mock(InputStream.class);

        // *** CRITICAL ADDITION: Enable mark/reset support for the mock ***
        when(mockRawInputStream.markSupported()).thenReturn(true);

        // Stub the sequence of bytes that GzipCompressorInputStream's constructor expects
        // for a valid GZIP header (RFC 1952). This includes 10 bytes:
        // ID1, ID2, CM, FLG, MTIME(4 bytes), XFL, OS.
        when(mockRawInputStream.read())
                .thenReturn(31) // GZIP ID1 (0x1f)
                .thenReturn(139) // GZIP ID2 (0x8b)
                .thenReturn(8) // Compression Method (8 = Deflate)
                .thenReturn(0) // Flags (0 means no optional header fields)
                .thenReturn(0) // MTIME byte 1
                .thenReturn(0) // MTIME byte 2
                .thenReturn(0) // MTIME byte 3
                .thenReturn(0) // MTIME byte 4
                .thenReturn(0) // Extra flags (XFL)
                .thenReturn(0) // Operating System (OS)
                .thenReturn(-1); // After the 10 header bytes, signify End Of File.
    }

    @Test
    void shouldBuildInputStream() throws IOException {

        var builder = GZipDecompressor.builder(mock(GzipCompressorInputStream.class));

        // when
        try (GzipCompressorInputStream in = builder.buildCompressorInputStream()) {

            // then
            assertThat(in).isNotNull();
        }
    }

    @Test
    void shouldBuildInputStreamWithParameters() throws IOException {

        var builder = GZipDecompressor.builder(mock(GzipCompressorInputStream.class));

        // when
        try (GzipCompressorInputStream in =
                builder.inputStreamBuilder().setDecompressConcatenated(true).buildInputStream()) {
            assertThat(in).isNotNull();
        }
    }
}
