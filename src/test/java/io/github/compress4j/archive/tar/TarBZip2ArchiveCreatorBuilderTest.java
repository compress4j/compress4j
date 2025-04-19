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
package io.github.compress4j.archive.tar;

import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.junit.jupiter.api.Test;

class TarBZip2ArchiveCreatorBuilderTest {

    @Test
    void shouldBuildArchiveOutputStream() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = spy(TarBZip2ArchiveCreator.builder(outputStream)
                .longFileMode(LONGFILE_POSIX)
                .bigNumberMode(BIGNUMBER_POSIX));

        // when
        try (TarArchiveOutputStream out = spy(builder.buildArchiveOutputStream())) {

            // then
            assertThat(out).isNotNull();

            then(builder).should().buildTarArchiveOutputStream(assertArg(o -> assertThat(o)
                    .isInstanceOf(BZip2CompressorOutputStream.class)
                    .extracting("blockSize100k")
                    .isEqualTo(9)));
        }
    }

    @Test
    void shouldBuildArchiveOutputStreamWithBlockSize() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = spy(TarBZip2ArchiveCreator.builder(outputStream)
                .compressorOutputStreamBuilder()
                .blockSize(5)
                .parentBuilder());

        // when
        try (TarArchiveOutputStream out = spy(builder.buildArchiveOutputStream())) {

            // then
            assertThat(out).isNotNull();

            then(builder).should().buildTarArchiveOutputStream(assertArg(o -> assertThat(o)
                    .isInstanceOf(BZip2CompressorOutputStream.class)
                    .extracting("blockSize100k")
                    .isEqualTo(5)));
        }
    }

    @Test
    void shouldNotAllowBlockSizeLowerOutOfRange() {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = TarBZip2ArchiveCreator.builder(outputStream).compressorOutputStreamBuilder();

        // when & then
        assertThatThrownBy(() -> builder.blockSize(-2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockSize(-2) < 1");
    }

    @Test
    void shouldNotAllowBlockSizeHigherOutOfRange() {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = TarBZip2ArchiveCreator.builder(outputStream).compressorOutputStreamBuilder();

        // when & then
        assertThatThrownBy(() -> builder.blockSize(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockSize(10) > 9");
    }
}
