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
package io.github.compress4j.archivers.tar;

import static java.util.zip.Deflater.BEST_COMPRESSION;
import static java.util.zip.Deflater.HUFFMAN_ONLY;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;

class TarGzArchiveCreatorBuilderTest {

    @Test
    void shouldBuildArchiveOutputStream() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = spy(TarGzArchiveCreator.builder(outputStream)
                .longFileMode(LONGFILE_POSIX)
                .bigNumberMode(BIGNUMBER_POSIX));

        // when
        try (TarArchiveOutputStream out = spy(builder.buildArchiveOutputStream())) {

            // then
            assertThat(out)
                    .isNotNull()
                    .extracting("longFileMode", "bigNumberMode")
                    .containsExactly(LONGFILE_POSIX, BIGNUMBER_POSIX);
            then(builder).should().buildTarArchiveOutputStream(assertArg(o -> assertThat(o)
                    .isInstanceOf(GzipCompressorOutputStream.class)));
        }
    }

    @Test
    void shouldBuildArchiveOutputStreamWithGzipParameters() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var now = Instant.now();
        var builder = spy(TarGzArchiveCreator.builder(outputStream)
                .compressorOutputStreamBuilder()
                .bufferSize(1024)
                .compressionLevel(BEST_COMPRESSION)
                .comment("comment")
                .deflateStrategy(HUFFMAN_ONLY)
                .fileName("test.tar.gz")
                .modificationTime(now.toEpochMilli())
                .operatingSystem(0)
                .parentBuilder()
                .longFileMode(LONGFILE_POSIX)
                .bigNumberMode(BIGNUMBER_POSIX));

        // when
        try (TarArchiveOutputStream out = spy(builder.buildArchiveOutputStream())) {

            // then
            assertThat(out)
                    .isNotNull()
                    .extracting("longFileMode", "bigNumberMode")
                    .containsExactly(LONGFILE_POSIX, BIGNUMBER_POSIX);
            then(builder).should().buildTarArchiveOutputStream(assertArg(o -> {
                assertThat(o)
                        .isInstanceOf(GzipCompressorOutputStream.class)
                        .extracting("deflateBuffer")
                        .isEqualTo(new byte[1024]);

                assertThat(o)
                        .isInstanceOf(GzipCompressorOutputStream.class)
                        .extracting("deflater")
                        .extracting("level", "strategy")
                        .containsExactly(BEST_COMPRESSION, HUFFMAN_ONLY);
            }));
        }
    }
}
