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
package io.github.compress4j.archive.compression.builder;

import static java.util.Collections.emptyMap;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.util.ReflectionUtils;

class TarGzArchiveOutputStreamBuilderTest {

    @Test
    void shouldBuildArchiveOutputStream() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var builder = spy(new TarGzArchiveOutputStreamBuilder(outputStream));

        // when
        try (TarArchiveOutputStream out = spy(builder.build())) {

            // then
            assertThat(out).isNotNull();
            var longFileMode = ReflectionUtils.tryToReadFieldValue(TarArchiveOutputStream.class, "longFileMode", out)
                    .toOptional();
            assertThat(longFileMode).isPresent().contains(LONGFILE_POSIX);

            var bigNumberMode = ReflectionUtils.tryToReadFieldValue(TarArchiveOutputStream.class, "bigNumberMode", out)
                    .toOptional();
            assertThat(bigNumberMode).isPresent().contains(BIGNUMBER_POSIX);

            verify(builder)
                    .buildTarArchiveOutputStream(
                            assertArg(o -> assertThat(o).isInstanceOf(GzipCompressorOutputStream.class)),
                            eq(emptyMap()));
        }
    }

    @Test
    void shouldBuildArchiveOutputStreamWithGzipParameters() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        Map<String, Object> options = Map.of("compression-level", 9);
        var builder = spy(new TarGzArchiveOutputStreamBuilder(outputStream, options));

        // when
        try (TarArchiveOutputStream out = spy(builder.build())) {

            // then
            assertThat(out).isNotNull();
            var longFileMode = ReflectionUtils.tryToReadFieldValue(TarArchiveOutputStream.class, "longFileMode", out)
                    .toOptional();
            assertThat(longFileMode).isPresent().contains(LONGFILE_POSIX);

            var bigNumberMode = ReflectionUtils.tryToReadFieldValue(TarArchiveOutputStream.class, "bigNumberMode", out)
                    .toOptional();
            assertThat(bigNumberMode).isPresent().contains(BIGNUMBER_POSIX);

            verify(builder)
                    .buildTarArchiveOutputStream(
                            assertArg(o -> assertThat(o).isInstanceOf(GzipCompressorOutputStream.class)),
                            eq(emptyMap()));
        }
    }

    @Test
    void shouldNotBuildArchiveOutputStreamWithUnknownOptions(@TempDir Path tempDir) throws IOException {
        // given
        var path = tempDir.resolve("archive.tar.gz");
        Map<String, Object> options = Map.of("something", 9);
        var builder = spy(new TarGzArchiveOutputStreamBuilder(path, options));

        // when
        assertThatThrownBy(builder::build).isInstanceOf(IOException.class).hasMessage("Cannot set option: something");
    }
}
