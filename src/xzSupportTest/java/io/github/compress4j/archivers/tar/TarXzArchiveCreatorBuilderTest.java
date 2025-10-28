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

import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.assertArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.tukaani.xz.LZMA2Options;

class TarXzArchiveCreatorBuilderTest {

    @Test
    void shouldBuildArchiveOutputStream() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = spy(TarXzArchiveCreator.builder(outputStream)
                .longFileMode(LONGFILE_POSIX)
                .bigNumberMode(BIGNUMBER_POSIX));

        // when
        try (var out = spy(builder.buildArchiveOutputStream())) {

            // then
            assertThat(out)
                    .isNotNull()
                    .extracting("longFileMode", "bigNumberMode")
                    .containsExactly(LONGFILE_POSIX, BIGNUMBER_POSIX);
            then(builder).should().buildTarArchiveOutputStream(assertArg(o -> assertThat(o)
                    .isInstanceOf(XZCompressorOutputStream.class)));
        }
    }

    @Test
    void shouldBuildArchiveOutputStreamWithXZParameters() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        LZMA2Options lzma2Options = new LZMA2Options(3);
        var builder = spy(TarXzArchiveCreator.builder(outputStream)
                .compressorOutputStreamBuilder()
                .lzma2Options(lzma2Options)
                .parentBuilder()
                .longFileMode(LONGFILE_POSIX)
                .bigNumberMode(BIGNUMBER_POSIX));

        // when
        try (var out = spy(builder.buildArchiveOutputStream())) {

            // then
            assertThat(out)
                    .isNotNull()
                    .extracting("longFileMode", "bigNumberMode")
                    .containsExactly(LONGFILE_POSIX, BIGNUMBER_POSIX);
            then(builder).should().buildTarArchiveOutputStream(assertArg(o -> assertThat(o)
                    .isInstanceOf(XZCompressorOutputStream.class)));
        }
    }
}
