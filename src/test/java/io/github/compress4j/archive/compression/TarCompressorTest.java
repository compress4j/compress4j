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
package io.github.compress4j.archive.compression;

import static java.time.Instant.now;
import static org.mockito.Mockito.*;

import io.github.compress4j.archive.compression.builder.TarArchiveOutputStreamBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;

class TarCompressorTest {

    @Test
    void shouldWriteSymlink() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var inputStream = mock(InputStream.class);

        // when
        TarArchiveOutputStream aOut = spy(new TarArchiveOutputStreamBuilder(outputStream).build());
        try (TarCompressor tarCompressor = new TarCompressor(aOut)) {

            tarCompressor.writeFileEntry(
                    "test", inputStream, 0, FileTime.from(now()), 0, Optional.of(Path.of("target")));

            // then
            verify(aOut, times(2)).putArchiveEntry(any());
        }
    }
}
