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

import static io.github.compress4j.assertion.AssertJMatcher.assertArgs;
import static java.nio.file.attribute.PosixFilePermission.*;
import static java.time.Instant.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.compress4j.archive.compression.builder.TarGzArchiveOutputStreamBuilder;
import io.github.compress4j.assertion.Compress4JAssertions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.attribute.FileTimes;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TarGzCompressorTest {

    @Test
    void shouldWriteEntry() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var inputStream = new ByteArrayInputStream("test".getBytes());

        // when
        var aOut = spy(new TarGzArchiveOutputStreamBuilder(outputStream).build());
        try (MockedStatic<IOUtils> mockIOUtils = mockStatic(IOUtils.class, CALLS_REAL_METHODS);
                TarGzCompressor tarCompressor = new TarGzCompressor(aOut)) {

            FileTime modTime = FileTime.from(now());
            @SuppressWarnings("OctalInteger")
            int mod = 0400;
            tarCompressor.writeFileEntry("test", inputStream, -1, modTime, mod, Optional.empty());

            // then
            mockIOUtils.verify(() -> IOUtils.copy(any(InputStream.class), any(OutputStream.class)));
            verify(aOut, times(2))
                    .putArchiveEntry(assertArgs(
                            e -> Compress4JAssertions.assertThat(e)
                                    .hasName("test")
                                    .hasLinkName("")
                                    .hasSize(4L)
                                    .hasMode(mod)
                                    .isNotSymbolicLink()
                                    .hasModTimeCloseToInSeconds(modTime),
                            e -> Compress4JAssertions.assertThat(e)
                                    .hasName("./PaxHeaders.X/test")
                                    .hasLinkName("")
                                    .hasSize(28L)
                                    .hasModTimeCloseToInSeconds(modTime)
                                    .hasMode(Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ))
                                    .isNotSymbolicLink()));
            verify(aOut, times(2)).closeArchiveEntry();
        }
    }

    @Test
    void shouldWriteEntryWithSymlink() throws IOException {
        // given
        var outputStream = mock(OutputStream.class);
        var inputStream = mock(InputStream.class);

        // when
        var aOut = spy(new TarGzArchiveOutputStreamBuilder(outputStream).build());
        try (MockedStatic<IOUtils> mockIOUtils = mockStatic(IOUtils.class);
                TarGzCompressor tarCompressor = new TarGzCompressor(aOut)) {

            Instant now = now();
            FileTime modTime = FileTime.from(now);
            tarCompressor.writeFileEntry("test", inputStream, 0, modTime, 0, Optional.of(Path.of("target")));

            // then
            mockIOUtils.verifyNoInteractions();
            verify(aOut, times(2))
                    .putArchiveEntry(assertArgs(
                            e -> Compress4JAssertions.assertThat(e)
                                    .hasName("test")
                                    .hasLinkName("target")
                                    .hasSize(0L)
                                    .hasModTimeCloseToInSeconds(FileTimes.toDate(modTime))
                                    .isSymbolicLink()
                                    .hasMode(Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ)),
                            e -> Compress4JAssertions.assertThat(e)
                                    .hasName("./PaxHeaders.X/test")
                                    .hasLinkName("")
                                    .hasSize(28L)
                                    .hasModTimeCloseToInSeconds(now)
                                    .hasMode(Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ))
                                    .isNotSymbolicLink()));
            verify(aOut, times(2)).closeArchiveEntry();
        }
    }
}